package com.nttdata.documentqa.controller;

import com.nttdata.documentqa.model.Citation;
import com.nttdata.documentqa.model.QueryResponse;
import com.nttdata.documentqa.exception.EmptyLibraryException;
import com.nttdata.documentqa.service.RagService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.ai.openai.api-key=test-key")
@AutoConfigureWebTestClient
class QueryControllerIntegrationTest {

    @Autowired
    private WebTestClient client;

    @MockitoBean
    private RagService ragService;

    @MockitoBean
    private VectorStore vectorStore;

    @Test
    void returnsGroundedAnswerAndBackendCitations() {
        when(ragService.answer("Can I cancel?")).thenReturn(Mono.just(new QueryResponse(
                "Cancellation is allowed within 30 days.",
                List.of(new Citation("doc-1", "policy.pdf", 7, null, null, "chunk-1")))));

        client.post().uri("/api/query")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"question\":\"Can I cancel?\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.answer").isEqualTo("Cancellation is allowed within 30 days.")
                .jsonPath("$.citations[0].documentId").isEqualTo("doc-1")
                .jsonPath("$.citations[0].filename").isEqualTo("policy.pdf")
                .jsonPath("$.citations[0].page").isEqualTo(7)
                .jsonPath("$.citations[0].chunkId").isEqualTo("chunk-1");
    }

    @Test
    void rejectsBlankQuestionsBeforeCallingTheRagService() {
        client.post().uri("/api/query")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"question\":\"  \"}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_REQUEST")
                .jsonPath("$.message").isEqualTo("Question must not be blank.");

        verify(ragService, never()).answer(anyString());
    }

    @Test
    void returnsStructuredConflictForAnEmptyDocumentLibrary() {
        when(ragService.answer("What is covered?"))
                .thenReturn(Mono.error(new EmptyLibraryException()));

        client.post().uri("/api/query")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"question\":\"What is covered?\"}")
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("EMPTY_DOCUMENT_LIBRARY")
                .jsonPath("$.message").isEqualTo("Upload at least one document before asking a question.");
    }

    @Test
    void returnsNoCitationsForAWeakRetrievalMatch() {
        when(ragService.answer("Who won the football match?"))
                .thenReturn(Mono.just(new QueryResponse(
                        "I could not find sufficient information in the uploaded documents to answer this question.",
                        List.of())));

        client.post().uri("/api/query")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"question\":\"Who won the football match?\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.citations").isEmpty();
    }
}
