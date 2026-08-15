# Document Ingestion and Retrieval Service — UC1

This Java 21/Spring Boot WebFlux service accepts PDF or TXT policy documents and answers questions using retrieval-augmented generation (RAG). Answers are generated only after semantic retrieval finds sufficiently relevant source chunks, and citations are constructed by the backend from retrieved metadata rather than by the language model.

## Prerequisites

- Java 21
- An OpenAI API key with access to embeddings and chat models
- No local Maven installation is required; Maven Wrapper is included

The current implementation uses Spring Boot 4.1.0, Spring AI 2.0.0, PDFBox 3.0.8, OpenAI `text-embedding-3-small`, and OpenAI `gpt-5-mini`.

## Configuration

Set the API key in the environment. Do not put a real key in `.env.example` or commit it.

PowerShell:

```powershell
$env:OPENAI_API_KEY = "your-key"
```

Bash:

```bash
export OPENAI_API_KEY="your-key"
```

Application settings are in `src/main/resources/application.yml`:

```yaml
app:
  documents:
    max-size: 10MB
    chunk-size: 600
    chunk-overlap: 100
  retrieval:
    top-k: 5
    minimum-score: 0.70
```

The initial similarity threshold is deliberately configurable. Score semantics and distributions vary by embedding model and vector-store implementation, so `0.70` should be calibrated using a representative retrieval evaluation set rather than treated as universal.

There are no Docker Compose or local database dependencies in this slice. Embedding and chat requests go directly to OpenAI; vectors are held in the process-local `SimpleVectorStore`.

## Run the service

PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Bash:

```bash
./mvnw spring-boot:run
```

The service starts at `http://localhost:8080`.

## Demonstrate the end-to-end slice

Run these commands from the repository root.

Upload the included sample policy:

```bash
curl -X POST http://localhost:8080/api/documents \
  -F "file=@samples/sample-policy.txt;type=text/plain"
```

Example response:

```json
{
  "documentId": "16e1f915-5825-45ec-8ae5-5e6ff9b95520",
  "filename": "sample-policy.txt",
  "chunksCreated": 1,
  "status": "INGESTED"
}
```

Ask a question grounded in that policy:

```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"question":"What happens if I cancel within 30 days?"}'
```

Example response:

```json
{
  "answer": "You may cancel within 30 calendar days after the policy start date. If no claim has been made, the premium is refunded, subject to any disclosed non-refundable administrative fee.",
  "citations": [
    {
      "documentId": "16e1f915-5825-45ec-8ae5-5e6ff9b95520",
      "filename": "sample-policy.txt",
      "page": null,
      "lineStart": 1,
      "lineEnd": 15,
      "chunkId": "cb026b70-e6d8-4c61-a6ad-76af66bc95a4"
    }
  ]
}
```

For a PDF citation, `page` contains the actual PDF page extracted by PDFBox. TXT citations use line-range metadata.

## Deliberate edge-case behavior

- Empty library: HTTP `409 Conflict` with code `EMPTY_DOCUMENT_LIBRARY`. The request conflicts with the current server state and can succeed after a document is uploaded.
- Weak/no retrieval match: HTTP `200` with an insufficient-information answer and an empty citation array. The LLM is not called.
- Oversized upload: HTTP `413 Content Too Large` with code `DOCUMENT_TOO_LARGE`.
- Unsupported type: HTTP `415 Unsupported Media Type` with code `UNSUPPORTED_DOCUMENT_TYPE`.
- Empty or unreadable document: HTTP `400 Bad Request` with a structured error.

## Architecture

Detailed reviewer documents:

- [Architecture and engineering decisions](docs/architecture.md)
- [Maintaining answer quality as the library grows](docs/scaling-answer-quality.md)

```text
multipart upload
  -> validation and bounded byte collection
  -> PDF/TXT extraction (PDF page boundaries preserved)
  -> configurable overlapping chunks
  -> OpenAI embeddings
  -> Spring AI SimpleVectorStore

question
  -> query embedding and top-K search
  -> configurable minimum-score gate
  -> no match: fixed answer, no LLM call, no citations
  -> match: labeled retrieved context + grounded system prompt
  -> OpenAI chat model
  -> backend constructs citations from retrieved chunks
```

PDFBox, embedding, vector-store, and chat calls are blocking boundaries and are isolated on Reactor's `boundedElastic` scheduler. The HTTP layer remains WebFlux without pretending those libraries are nonblocking.

The important separation is visible in the service layer:

- `RetrievalService` owns relevance and weak-match decisions.
- `GroundingPromptFactory` formats only retrieved passages as model context.
- `AnswerGenerator` is the replaceable external chat boundary.
- `RagService` orchestrates generation and constructs trusted citations.
- `DocumentChunkRepository` isolates the vector-store implementation.

## Tests

```powershell
.\mvnw.cmd test
```

The suite mocks external AI boundaries. It covers validation, oversized files, PDF page metadata, overlapping chunks, semantic-result mapping, empty library, weak retrieval without generation, grounded prompts, citation construction, API responses, and a deterministic upload-to-cited-answer end-to-end path.

## Known limitations

- `SimpleVectorStore` is in memory: documents disappear on restart and similarity search is a linear scan. It is suitable for this assessment slice, not tens of thousands of files.
- TXT line metadata currently represents the extracted source block; chunk-specific line ranges could be made more precise.
- Chunking uses whitespace-delimited words as an understandable token approximation. Production code would use the selected model tokenizer and paragraph-aware boundaries.
- The initial relevance threshold has not yet been calibrated against a labeled evaluation set.
- All retrieved chunks supplied to the model are returned as citations. A production system could require structured claim-to-source mappings and validate each selected source against the retrieved set.
- There is no authentication, persistence, OCR, malware scanning, duplicate detection, or document versioning; these were deliberately left out of the three-day assessment slice.

## AI tooling disclosure

AI-assisted coding was used to scaffold and review the implementation, verify current official Spring Boot/Spring AI/PDFBox APIs, propose tests, and draft documentation. The resulting code was compiled and the test suite was run locally. No API keys or credentials are stored in the repository.
