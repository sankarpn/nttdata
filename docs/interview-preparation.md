# UC1 Technical Interview Questions and Answers

This guide is written as a rehearsal aid for the 45-minute engineering discussion. The answers describe the code as it exists today, acknowledge its deliberate limits, and explain what would change in production.

## One-minute project summary

**Question: Walk me through what you built.**

I built a Java 21 and Spring Boot WebFlux service with two endpoints. `POST /api/documents` accepts PDF or TXT files, validates the type and size, extracts text while retaining page or line metadata, creates overlapping chunks, generates OpenAI embeddings, and writes them to Spring AI's in-memory vector store. `POST /api/query` embeds the question, retrieves the top semantically similar chunks above a configurable relevance threshold, and either returns an honest no-match response or supplies only those chunks to the chat model. The backend—not the LLM—constructs citations from retrieved chunk metadata. PDFBox, embedding, vector search, and chat calls are blocking boundaries, so they run on Reactor's bounded-elastic scheduler.

## Required engineering-review questions

### 1. How do you decide whether a passage is relevant?

I use semantic similarity from the same embedding model for stored chunks and the question. Retrieval asks for a configurable top K, currently five, and also applies a configurable minimum score, currently `0.70`. A chunk must clear that threshold to be treated as relevant.

The key point is that “nearest” does not automatically mean “relevant.” The threshold is an initial operating value, not a universal truth. Similarity scores depend on the embedding model, corpus, and vector-store implementation. Before production, I would create a labeled evaluation set of relevant, irrelevant, and ambiguous question-passage pairs and select the threshold using retrieval and no-answer metrics.

### 2. What happens when even the best retrieval result is weak?

The vector store may always return a nearest neighbor, even for an unrelated question. If no chunk clears the minimum score, `RetrievalService` returns `NO_MATCH`. `RagService` then returns a fixed insufficient-information message with an empty citation list.

Most importantly, the LLM is not called on that path. This prevents unrelated context from being turned into a confident-sounding answer and avoids unnecessary cost and latency. A unit test verifies that the answer generator is never invoked for a weak match.

### 3. Where could the LLM produce unsupported claims?

The LLM could still combine context incorrectly, overlook a qualification, or add outside knowledge despite the prompt. Prompt grounding reduces that risk but does not prove factual faithfulness.

I reduce the opportunity for unsupported claims by placing retrieval before generation, rejecting weak retrieval, sending only retrieved text, using an explicit context-only system instruction, and keeping temperature low. Citations do not come from model output. In production I would add claim-to-source structured output, validate source identifiers against the retrieved set, run faithfulness evaluations, and consider a post-generation entailment check for high-risk answers.

### 4. How do you reduce hallucination?

I use several layers rather than relying on one prompt:

1. Retrieve evidence before generation.
2. Require candidates to pass a configurable relevance gate.
3. Skip generation entirely for weak matches.
4. Supply only retrieved context and the user's question.
5. Tell the model not to use outside knowledge or invent facts.
6. Construct citations from backend metadata.
7. Test the weak-match, grounding, and citation paths independently.

The remaining risk is unsupported wording inside the answer. A production version would measure groundedness on an evaluation set and validate claim-level source attribution.

### 5. What changes at roughly 100 times the document volume?

The in-memory linear-scan vector store would be the first major change. I would move to a persistent approximate-nearest-neighbor index and add metadata filters for tenant, policy type, jurisdiction, product, effective date, version, and language. That prevents a semantically similar but inapplicable document from winning.

I would evaluate structure-aware chunking, combine vector and lexical/BM25 retrieval, fuse candidate rankings, and rerank a larger candidate set before selecting a small prompt context. I would also add document hashes, version lifecycle rules, duplicate detection, and retrieval-quality telemetry. Every embedding-model or index change would run against the same labeled evaluation set before release.

## Architecture and implementation questions

### Why did you choose `SimpleVectorStore` instead of Chroma?

The goal was the fastest reliable end-to-end slice that still performs genuine embedding and semantic search. `SimpleVectorStore` removes Docker and database setup, making the reviewer path simpler. I hid it behind `DocumentChunkRepository`, so persistence can be introduced without changing controllers, extraction, chunking, or RAG orchestration.

The trade-off is explicit: vectors disappear on restart and search is a linear memory scan. I would not use it for tens of thousands of files.

### Is this a real RAG implementation?

Yes. Documents and questions are embedded, semantic retrieval selects context, a relevance gate can stop the pipeline, and only retrieved context is sent to the chat model. The service does not send the raw question directly to the model and ask it to answer from general knowledge.

### Why separate retrieval, prompt construction, generation, and citation construction?

Each stage enforces a different correctness rule:

- `RetrievalService` decides whether evidence is strong enough.
- `GroundingPromptFactory` controls exactly what evidence the model receives.
- `AnswerGenerator` isolates the external LLM SDK.
- `RagService` constructs citations from trusted backend objects.

That separation makes the weak-match path testable, prevents the LLM from controlling citations, and allows providers or vector stores to change without rewriting the entire workflow.

### How do you know citations are trustworthy?

PDF extraction produces one `PageContent` per actual PDF page. Each resulting chunk copies the backend-assigned document ID, filename, page number, and chunk ID. TXT files use line metadata instead of pretending they have pages.

The model sees source labels for context but is told not to output citations. After generation, `RagService` maps retrieved chunks into `Citation` records. Therefore a model-generated page number cannot enter the API response through the normal code path.

### Could the citations still be misleading?

Yes. The metadata is authentic, but the current implementation cites every retrieved chunk supplied to the model. That is conservative and may over-cite if only one passage supports a particular sentence. Production could require structured claim-to-source mappings and accept only identifiers that were present in the retrieved context.

### Why use 600-word chunks with 100-word overlap?

It is a simple, explainable starting point. Chunks that are too small may retrieve a phrase without its conditions or exceptions. Chunks that are too large become less specific and consume more prompt tokens. Overlap reduces the chance that an important clause is split exactly at a boundary.

The implementation uses words as a token approximation. Production would use the chosen model's tokenizer and likely respect headings, paragraphs, clauses, and tables. The values would be selected through retrieval evaluation rather than intuition alone.

### Why keep PDF chunks within page boundaries?

It preserves unambiguous page citations. If a chunk freely crossed pages, one citation page might not identify all its text. Page-local chunks make provenance straightforward. The trade-off is that a clause split across a page break may lose some context; overlap across adjacent page-aware units or structure-aware parsing could address that later.

### What happens if embedding storage fails during upload?

The vector store is written before the repository's local chunk map is updated. If embedding or vector insertion throws, the request fails and the local library state is not marked as successfully populated. With an external vector database, I would use idempotent document IDs, ingestion statuses, retryable jobs, and cleanup/reconciliation for partial batches.

### Why generate UUIDs for documents and chunks?

They provide stable identifiers independent of filenames, which are neither unique nor immutable. Those IDs connect retrieval results to citation metadata. At scale I would also store a content hash and version identity so duplicate uploads and superseded documents can be managed deliberately.

## WebFlux and concurrency questions

### Is the whole pipeline nonblocking?

No, and the code does not pretend it is. WebFlux handles HTTP reactively, but PDFBox and the current Spring AI embedding, vector-store, and chat calls are synchronous. Those operations are scheduled on `boundedElastic`, keeping them away from Netty event-loop threads.

### Why use `boundedElastic`?

It is Reactor's scheduler intended for bounded blocking work. It protects event-loop responsiveness without creating an unbounded thread per request. At larger scale I would also add concurrency limits, timeouts, provider rate-limit handling, and possibly move ingestion to a controlled asynchronous job pipeline.

### Why accumulate the whole upload in memory?

The assessment caps files at 10 MB, so bounded accumulation keeps extraction simple and prevents unbounded memory use. The code counts bytes while consuming buffers and fails as soon as the configured maximum is exceeded.

For larger documents or higher concurrency, I would stream to a controlled temporary file or object store and let extraction read from that resource. I would also enforce request limits at the server or gateway layer as defense in depth.

### Could concurrent uploads cause problems?

The local chunk map uses a concurrent collection, and `SimpleVectorStore` is designed for concurrent in-memory use. However, there is no ingestion idempotency or per-document transaction. Production would use stable ingestion jobs, document states, idempotency keys or hashes, batch writes, and reconciliation for partial failures.

## API and error-handling questions

### Why return `409 Conflict` for an empty library?

The query JSON is valid, but the operation conflicts with current server state because no documents are available. Uploading a document makes the same request viable. `422 Unprocessable Content` would also be defensible; the more important point is that the response is deliberate, structured, documented, and tested.

### Why return HTTP 200 for a weak match?

The service successfully processed a valid question and determined that the library lacks sufficient evidence. That is a domain result rather than a transport or validation failure. The response makes the outcome explicit and returns no citations.

### How are unsupported file types checked?

The validator checks both the filename extension and compatible media type. Only PDF and TXT are accepted. `application/octet-stream` is allowed when the extension is supported because some clients do not send a precise media type. A production service would add content-signature detection, malware scanning, and stricter file-policy enforcement.

### How are empty documents handled?

Extraction may succeed technically but produce no usable text—for example, an image-only PDF without OCR. The chunker ignores blank content, and ingestion rejects the document if no chunks remain. OCR was deliberately left out of the assessment scope.

## Testing questions

### How do the tests avoid paid API calls?

Unit and controller tests mock the external AI boundaries. The end-to-end test supplies a deterministic hashed-trigram embedding model and a recording answer generator only in its Spring test configuration. It still exercises the real upload endpoint, extraction, chunking, `SimpleVectorStore`, retrieval gate, prompt construction, query endpoint, and citation response.

### What are the highest-value tests?

The most important tests prove behavior rather than framework wiring:

- A two-page PDF retains the correct page number for each extracted passage.
- Oversized input fails at the configured limit.
- Empty-library queries fail deliberately.
- Weak retrieval returns no confident answer and never calls the LLM.
- Strong retrieval sends the retrieved text to the grounding prompt.
- Citations match the original retrieved chunk metadata.
- The deterministic end-to-end path uploads a sample policy and returns a cited answer.

### What would you test next?

I would add a small retrieval evaluation corpus with answerable and unanswerable questions, corrupted/encrypted PDF cases, multiple chunks with ranking assertions, duplicate uploads, provider timeout/error mapping, and a live smoke test guarded by an opt-in profile. I would keep ordinary CI independent of real API credentials.

## Production-evolution questions

### What is the first production improvement you would make?

Persistent document and vector storage with document status/version metadata. Losing all vectors on restart is the largest current operational limitation. I would choose the store based on the team's existing platform and operational skills rather than introducing a novel database solely for this feature.

### How would you support multiple insurers, teams, or users?

I would add tenant ownership to the document and every chunk, enforce authorization before ingestion and query, and require a tenant filter in every vector search. Filtering after global retrieval would be unsafe because unauthorized passages could influence ranking or prompt context.

### How would you monitor quality?

I would record retrieval latency, result scores, applied metadata filters, selected document versions, no-match rate, reranker scores, prompt token count, citation coverage, and user feedback. Sensitive document text should not be logged by default. Offline evaluation would remain the release gate; production telemetry would reveal distribution changes and new failure modes.

### How would you handle model changes?

Embedding-model changes generally require re-embedding the corpus because vector spaces are not interchangeable. I would version the embedding model and index, build a new index alongside the old one, evaluate it, and cut traffic over safely. Chat-model changes do not require re-embedding but still require groundedness and answer-quality regression tests.

### What did you deliberately leave out?

Authentication, user management, OCR, malware scanning, durable storage, duplicate/version management, production observability, Kubernetes, and complex provider abstractions. Those are valid production concerns, but they were not necessary to demonstrate the assignment's core judgment: real retrieval, honest weak-match behavior, model grounding, and trustworthy citations.

## Closing answer

**Question: If you had one more day, what would you do?**

I would first run the full slice against a small labeled set of real-looking policy questions and tune chunking and the relevance threshold based on measured retrieval results. Next I would persist the vectors and document metadata so the demo survives restarts. I would avoid adding more infrastructure until those quality and durability gaps were addressed, because they matter more to the assessment than additional framework layers.
