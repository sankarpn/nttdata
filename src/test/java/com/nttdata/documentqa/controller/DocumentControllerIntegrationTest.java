package com.nttdata.documentqa.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.ai.openai.api-key=test-key")
@AutoConfigureWebTestClient
class DocumentControllerIntegrationTest {

    @Autowired
    private WebTestClient client;

    @MockitoBean
    private VectorStore vectorStore;

    @Test
    void ingestsTextAndReturnsDocumentIdentityAndChunkCount() {
        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("file", new NamedByteArrayResource("sample-policy.txt",
                        "Cancellation is allowed within thirty days.".getBytes()))
                .contentType(MediaType.TEXT_PLAIN);

        client.post().uri("/api/documents")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(body.build())
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.documentId").isNotEmpty()
                .jsonPath("$.filename").isEqualTo("sample-policy.txt")
                .jsonPath("$.chunksCreated").isEqualTo(1)
                .jsonPath("$.status").isEqualTo("INGESTED");
    }

    private static final class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        private NamedByteArrayResource(String filename, byte[] bytes) {
            super(bytes);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
