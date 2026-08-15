# Standalone Milestone Prompts for Rebuilding UC1

This playbook contains copy-paste prompts for recreating the entire Document Ingestion and Retrieval Service from an empty repository. The prompts do not rely on this repository, prior chat context, or unstated decisions.

Use the prompts in order. Start a fresh coding-agent conversation with the kickoff prompt, then send one milestone prompt at a time. Review the code and test results before continuing. Do not ask the agent to implement all milestones in a single response.

## Target outcome

The finished project is a Java 21, Maven, Spring Boot WebFlux backend for an insurance-company document question-answering use case. It must:

- Accept PDF and TXT uploads.
- Reject unsupported, oversized, unreadable, and empty documents.
- Preserve PDF page numbers and sensible TXT source metadata.
- Create configurable overlapping chunks.
- Generate embeddings and store chunks in a semantic vector store.
- Retrieve relevant chunks for a natural-language question.
- Reject weak retrieval before calling the LLM.
- Ground generation only in retrieved context.
- Construct citations from backend metadata rather than model output.
- Run locally with clear instructions and no committed secrets.
- Include focused tests, a deterministic end-to-end test, architecture documentation, a scaling-quality design note, and interview preparation material.

## Prompt 0 — Project kickoff and working rules

```text
I am completing a take-home technical assessment for a Senior Full-Stack Java Engineer role. Build Use Case 1 only: a Document Ingestion and Retrieval Service for an insurance company.

Act as a senior Java/Spring engineer pair-programming with me. Implement directly in the current workspace. I need working, clean, explainable code, not an overengineered production platform.

Project constraints:
- Java 21
- Spring Boot and Maven Wrapper
- Spring WebFlux for the HTTP API
- Spring AI where practical
- Apache PDFBox for PDFs
- OpenAI embeddings and chat models, with the API key read only from OPENAI_API_KEY
- JUnit 5
- Three-day take-home scope
- No Kubernetes, Terraform, authentication, frontend, microservices, or unrelated infrastructure
- Never commit secrets
- Do not commit or push unless I explicitly request it

Engineering priorities, in order:
1. Correct working end-to-end slice
2. Grounded retrieval
3. Trustworthy backend-owned citations
4. Weak-match handling
5. Understandable Java/Spring code
6. Structured error handling
7. Focused tests
8. Reviewer-ready documentation

Working rules:
- First inspect the workspace and report what exists.
- Verify current official Spring Boot, Spring AI, and PDFBox APIs before choosing versions or classes. Do not copy obsolete tutorial APIs.
- Work in milestones. Do not implement later milestones early.
- Run compilation and tests after every meaningful milestone and fix actual failures.
- Keep controllers thin and separate extraction, chunking, retrieval, prompt construction, generation, and citation construction.
- Identify blocking libraries honestly and isolate them from Netty event-loop threads with boundedElastic where appropriate.
- After each milestone, summarize: what changed, tests run, important decision, trade-off, production alternative, and suggested commit message.

For this kickoff, inspect the empty/current workspace, verify the current official dependency versions, propose the smallest first milestone, and stop before implementing it.
```

## Prompt 1 — Initialize the WebFlux project

```text
Implement milestone 1: initialize the backend project.

Requirements:
- Java 21
- Current stable Spring Boot compatible with Java 21
- Maven project with Maven Wrapper
- Spring WebFlux
- Bean validation
- Base package: com.nttdata.documentqa
- Main class: DocumentQaServiceApplication
- Externalized YAML configuration
- Clean package skeleton for controller, service, document, model, exception, and config
- Do not add Spring AI, an LLM, embeddings, vector storage, or query functionality yet

Add a context-load test. Run the test suite with Java 21 and fix all failures. Do not commit.

Acceptance criteria:
- ./mvnw test or .\\mvnw.cmd test succeeds
- The application starts as a WebFlux application
- No secrets or unnecessary dependencies exist

Suggested commit after review:
feat: initialize Spring Boot document service
```

## Prompt 2 — Upload validation and bounded file reading

```text
Implement milestone 2: the HTTP upload boundary and validation.

Create:
- POST /api/documents
- Content-Type: multipart/form-data
- Multipart part name: file

Support only PDF and TXT. Validate a compatible filename extension and media type. application/octet-stream may be accepted when the extension is supported because some clients send a generic type.

Externalize the maximum upload size under:

app:
  documents:
    max-size: 10MB

Consume WebFlux DataBuffer content safely, count bytes while reading, release buffers, and fail as soon as the configured maximum is exceeded. Do not repeat a hardcoded limit throughout the code.

Add structured errors:
- DOCUMENT_TOO_LARGE -> HTTP 413 Content Too Large
- UNSUPPORTED_DOCUMENT_TYPE -> HTTP 415 Unsupported Media Type

Use @RestControllerAdvice. Keep the controller thin. Do not extract, chunk, embed, or store document contents yet.

Tests must cover accepted PDF/TXT metadata, unsupported types, mismatched extension/media type, and oversized upload. Run all tests and fix failures. Do not commit.

Suggested commit:
feat: add bounded PDF and text upload validation
```

## Prompt 3 — PDF/TXT extraction with citation provenance

```text
Implement milestone 3: document extraction with trustworthy source provenance.

Use Apache PDFBox's current official API. Introduce a small DocumentExtractor interface with PDF and TXT implementations.

PDF requirements:
- Extract one page at a time, not one giant string.
- Preserve the actual one-based PDF page number.
- Invalid/unreadable PDFs return a structured INVALID_DOCUMENT error with HTTP 400.

TXT requirements:
- Decode UTF-8.
- Preserve sensible line metadata because TXT has no real page number.
- Never invent a page number for TXT.

Create a PageContent model that can represent either PDF page metadata or TXT line metadata. Reject documents where extraction produces no usable text with code EMPTY_DOCUMENT and HTTP 400.

PDFBox is blocking. Run extraction away from the event loop using Mono/fromCallable or an equivalent boundedElastic boundary. Keep the reactive design simple and explainable.

Add a test that programmatically creates a two-page PDF and proves each extracted passage has the correct page number and text. Add TXT and empty-document tests as useful. Run the full suite. Do not commit.

Suggested commit:
feat: add page-aware PDF and text extraction
```

## Prompt 4 — Configurable overlapping chunking

```text
Implement milestone 4: configurable chunking with citation metadata.

Externalize:

app:
  documents:
    chunk-size: 600
    chunk-overlap: 100

Use a simple word-window strategy. Validate that chunk size is positive and overlap is nonnegative and smaller than chunk size. Keep PDF chunks within their source page so citations remain unambiguous. TXT chunks must retain available line metadata.

Create a DocumentChunk record containing at least:
- chunkId
- documentId
- filename
- pageNumber, nullable for TXT
- lineStart and lineEnd, nullable for PDF
- chunkIndex
- text

Generate a document UUID per successful upload and a chunk UUID per chunk. Return:

{
  "documentId": "uuid",
  "filename": "policy.pdf",
  "chunksCreated": 42,
  "status": "INGESTED"
}

Introduce a small DocumentChunkRepository abstraction, but use a simple temporary in-memory implementation without embeddings in this milestone.

Tests must prove overlap behavior, sequential chunk indexes, and preservation of document/filename/page metadata. Run all tests. Do not commit.

Suggested commit:
feat: add configurable chunking with citation metadata
```

## Prompt 5 — Real embeddings and semantic vector retrieval

```text
Implement milestone 5: real embeddings and semantic retrieval. Verify all Spring AI dependency names and APIs against the current official documentation before editing the build.

Use:
- The current compatible Spring AI BOM
- OpenAI text-embedding-3-small by default
- OPENAI_API_KEY from the environment
- Spring AI SimpleVectorStore for this assessment slice

Do not add Chroma or Docker unless SimpleVectorStore cannot support a genuine embedding and cosine-similarity flow. Clearly document that the in-memory store is demo-only, loses data on restart, and performs a linear scan.

Adapt DocumentChunkRepository so ingestion converts every chunk to a Spring AI Document and stores its embedding. Preserve backend metadata in the vector document and retain a reliable mapping to the original DocumentChunk.

Create RetrievalProperties:

app:
  retrieval:
    top-k: 5
    minimum-score: 0.70

Create RetrievalService that:
- Rejects a query against an empty library before embedding/searching.
- Executes semantic search with configurable top K and minimum score.
- Returns MATCH with retrieved chunks and scores, or NO_MATCH with no chunks.
- Runs synchronous embedding/vector SDK work on boundedElastic.

Add EMPTY_DOCUMENT_LIBRARY as a structured error. Use HTTP 409 Conflict and explain that the request is valid but conflicts with current server state until a document is uploaded.

Tests must mock external AI boundaries and prove:
- Stored vector documents contain trusted citation metadata.
- Search scores map back to original chunks.
- Empty library stops before search.
- Weak search returns NO_MATCH with no chunks.
- Successful search preserves citation metadata.

Do not add the LLM query endpoint yet. Run all tests. Do not commit.

Suggested commit:
feat: add semantic vector storage and weak-match retrieval
```

## Prompt 6 — Grounded RAG query endpoint

```text
Implement milestone 6: the grounded query endpoint and backend-owned citations.

Create:

POST /api/query
Content-Type: application/json

Request:
{
  "question": "What happens if I cancel within 30 days?"
}

Validate that question is not blank and return a structured INVALID_REQUEST error with HTTP 400.

Enable the current OpenAI chat integration, defaulting to gpt-5-mini with a low temperature such as 0.1. Verify the current Spring AI ChatClient API against official documentation.

Maintain explicit separation:
- RetrievalService: evidence selection and weak-match decision
- GroundingPromptFactory: system prompt and labeled retrieved passages
- AnswerGenerator: replaceable external LLM boundary
- RagService: orchestration and backend citation construction

The system prompt must state:
- Answer only from supplied context.
- If context is insufficient, say the uploaded documents do not contain enough information.
- Do not use outside knowledge.
- Do not invent facts.
- Do not create citations because the backend adds them.

Label each retrieved passage with filename, page or line range, and chunk ID. Send only the retrieved passages and user's question to the model.

Critical weak-match behavior:
- If RetrievalService returns NO_MATCH, return exactly an honest insufficient-information answer and citations: [].
- Do not invoke the LLM on that path.

Response citation fields:
- documentId
- filename
- page
- lineStart
- lineEnd
- chunkId

Construct citations after generation exclusively from retrieved DocumentChunk metadata. Do not parse citation IDs or page numbers from model output. Run synchronous chat calls on boundedElastic.

Tests must prove:
- Blank question validation.
- Empty-library API response.
- Weak match produces no citations and never calls AnswerGenerator.
- Strong match sends labeled retrieved text to the grounding prompt.
- Returned citations exactly match retrieved chunk metadata.
- Query controller response shape.

Run all tests. Do not commit.

Suggested commit:
feat: add grounded RAG query endpoint with trusted citations
```

## Prompt 7 — Deterministic end-to-end test

```text
Implement milestone 7: a deterministic end-to-end test that does not require a paid API.

Add a fictional sample insurance policy at samples/sample-policy.txt. It should include an answerable cancellation-within-30-days clause and a few unrelated policy clauses.

Create a Spring Boot integration test that exercises the real path:

multipart upload
-> TXT extraction
-> chunking
-> vector storage
-> semantic retrieval
-> relevance gate
-> grounded prompt
-> answer
-> citation response

In the test context only:
- Replace the production EmbeddingModel with a deterministic local implementation, such as a normalized hashed token or character-trigram vector.
- Replace AnswerGenerator with a recording deterministic fake.
- Keep the real SimpleVectorStore and application services.

The test must upload samples/sample-policy.txt through POST /api/documents, query known information through POST /api/query, assert a nonempty backend citation for sample-policy.txt, and verify that the retrieved policy text and question reached the grounding prompt.

Do not change production behavior to accommodate the test. Run the complete suite and fix all failures. Do not commit.

Suggested commit:
test: add deterministic upload-to-cited-answer integration test
```

## Prompt 8 — Reviewer-ready README

```text
Implement milestone 8: create a reviewer-ready README for UC1.

The README must include:
- Project purpose
- Prerequisites
- OPENAI_API_KEY setup for PowerShell and Bash
- Exact technology versions actually used
- Externalized configuration values
- How to start dependencies; explicitly say when there are no Docker/local database dependencies
- How to run the service with Maven Wrapper
- Runnable curl upload command using samples/sample-policy.txt
- Runnable curl query command
- Example successful JSON responses
- PDF versus TXT citation behavior
- Empty-library, weak-match, oversized, unsupported, and empty-document behavior
- Architecture summary
- Blocking/reactive boundary explanation
- How to run tests
- Known limitations
- AI tooling disclosure

Do not claim features that are not implemented. Do not include a real credential. Remove irrelevant generated help text. Run a secret scan and Markdown/diff sanity check. Do not commit.

Suggested commit:
docs: add UC1 setup and end-to-end demonstration
```

## Prompt 9 — Architecture and required scaling note

```text
Implement milestone 9: add reviewer-facing design documentation under docs/ and link it from the README.

Create docs/architecture.md containing:
- Purpose and assessment scope
- System-context Mermaid diagram
- Component responsibility table
- Upload sequence diagram
- Query/grounding sequence diagram
- Citation trust model
- Relevance and weak-match decision
- WebFlux versus blocking boundaries
- Key decisions in this format: Decision, Why, Trade-off, Production direction
- Security and operational items deliberately out of scope
- Test/verification strategy

Create docs/scaling-answer-quality.md as an approximately half-page answer to:
“How would you keep answer quality from degrading as the document library grows to tens of thousands of files?”

The scaling note must directly discuss:
- Persistent approximate-nearest-neighbor vector indexing
- Metadata filtering, including policy version/effective date/jurisdiction
- Duplicate and superseded documents
- Structure-aware/token-aware chunking
- Hybrid lexical plus vector retrieval
- Candidate fusion and reranking
- Top-K and threshold calibration
- Labeled retrieval evaluation set
- Metrics for retrieval and no-answer quality
- Grounding and backend citation validation
- Retrieval-quality observability without leaking sensitive text

Clearly distinguish current behavior from production evolution. Verify Mermaid fences, links, and Markdown formatting. Do not commit.

Suggested commit:
docs: add architecture and retrieval scaling notes
```

## Prompt 10 — Interview preparation guide

```text
Implement milestone 10: create docs/interview-preparation.md and link it from the README.

Write answers in first person so I can rehearse them. Keep them specific to the implemented code rather than generic RAG theory.

Include:
- A one-minute project walkthrough
- How relevance is decided
- What happens when the best match is weak
- Where the LLM can make unsupported claims
- How hallucination is reduced
- What changes at 100x document volume
- Why SimpleVectorStore was chosen over Chroma
- Why this qualifies as real RAG
- Why retrieval, prompt construction, generation, and citations are separate
- Why citations are trustworthy and how they can still over-cite
- Chunk-size and overlap reasoning
- PDF page-boundary reasoning
- Failure behavior during embedding/storage
- WebFlux and boundedElastic explanation
- Upload memory-limit trade-offs
- API status choices
- Highest-value tests and why external APIs are mocked
- First production improvements
- Tenant filtering, monitoring, and model migration
- Deliberately omitted scope
- “What would you do with one more day?”

Use concise answers that show decision, rationale, trade-off, and production alternative. Run a Markdown/diff sanity check. Do not commit.

Suggested commit:
docs: add technical interview preparation guide
```

## Prompt 11 — Final quality gate

```text
Perform the final UC1 quality gate. Do not add new features unless needed to fix a demonstrated issue.

Checks:
1. Run the complete Java 21 Maven test suite.
2. Run a clean package build.
3. Confirm no test calls a paid external API.
4. Confirm no secrets, API keys, generated target files, IDE files, or temporary QA artifacts are tracked.
5. Confirm both endpoints and all documented response fields match the code.
6. Confirm PDF citations use actual one-based pages and TXT citations do not invent pages.
7. Confirm weak retrieval cannot call the LLM.
8. Confirm citations are built only from retrieved backend metadata.
9. Confirm README commands and relative links are valid.
10. Confirm architecture diagrams render syntactically and the scaling note answers the required question.
11. Report git status and list proposed small commit boundaries if the repository has not been committed.

Fix concrete failures, rerun affected checks, and provide a concise final readiness report. Do not commit or push unless I explicitly request it.
```

## Optional prompt — Create meaningful Git history

Use this only if milestones were implemented without commits and you want the agent to commit the current verified state. It cannot reconstruct genuinely incremental history without rewriting commits, so prefer committing after each milestone during the rebuild.

```text
The implementation and documentation are complete and verified. Review git status and diff for secrets or unrelated files. If clean, create a commit for the current reviewed changes using a concise conventional-commit message. Do not amend, squash, rebase, force-push, or rewrite existing history. Do not push until I provide the target repository and explicitly ask you to push.
```

## Completion checklist

The rebuild is complete when all of the following are true:

- Uploading the sample TXT file returns `INGESTED` and at least one chunk.
- A two-page PDF extraction test proves correct page metadata.
- Querying before upload returns `EMPTY_DOCUMENT_LIBRARY`.
- An unrelated question returns the insufficient-information response with no citations and no LLM call.
- An answerable question passes retrieved text to the model and returns backend-owned citations.
- The deterministic end-to-end test passes without external credentials.
- The entire test suite and package build pass on Java 21.
- README, architecture, scaling, and interview documents exist and agree with the code.
- No secrets or build artifacts are tracked.
