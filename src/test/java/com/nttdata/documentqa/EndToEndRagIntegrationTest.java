package com.nttdata.documentqa;

import com.nttdata.documentqa.service.AnswerGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.ai.openai.api-key=test-key",
        "app.retrieval.minimum-score=0.05"
})
@AutoConfigureWebTestClient
@Import(EndToEndRagIntegrationTest.DeterministicAiConfiguration.class)
class EndToEndRagIntegrationTest {

    @Autowired
    private WebTestClient client;

    @Autowired
    private RecordingAnswerGenerator answerGenerator;

    @Test
    void uploadsSamplePolicyAndReturnsAnAnswerWithTheRetrievedCitation() {
        MultipartBodyBuilder upload = new MultipartBodyBuilder();
        upload.part("file", new FileSystemResource("samples/sample-policy.txt"))
                .contentType(MediaType.TEXT_PLAIN);

        client.post().uri("/api/documents")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(upload.build())
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.filename").isEqualTo("sample-policy.txt")
                .jsonPath("$.chunksCreated").isEqualTo(1);

        client.post().uri("/api/query")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"question\":\"What happens if I cancel within 30 days?\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.answer").isEqualTo("You may cancel within 30 days, subject to the policy terms.")
                .jsonPath("$.citations").isArray()
                .jsonPath("$.citations[0].documentId").isNotEmpty()
                .jsonPath("$.citations[0].filename").isEqualTo("sample-policy.txt")
                .jsonPath("$.citations[0].lineStart").isEqualTo(1)
                .jsonPath("$.citations[0].chunkId").isNotEmpty();

        assertThat(answerGenerator.lastUserPrompt)
                .contains("policyholder may cancel this policy within 30 calendar days")
                .contains("What happens if I cancel within 30 days?");
    }

    @TestConfiguration
    static class DeterministicAiConfiguration {
        @Bean
        @Primary
        EmbeddingModel deterministicEmbeddingModel() {
            return new HashedEmbeddingModel();
        }

        @Bean
        @Primary
        RecordingAnswerGenerator recordingAnswerGenerator() {
            return new RecordingAnswerGenerator();
        }
    }

    static final class RecordingAnswerGenerator implements AnswerGenerator {
        private volatile String lastUserPrompt;

        @Override
        public String generate(String systemPrompt, String userPrompt) {
            this.lastUserPrompt = userPrompt;
            return "You may cancel within 30 days, subject to the policy terms.";
        }
    }

    static final class HashedEmbeddingModel implements EmbeddingModel {
        private static final int DIMENSIONS = 128;

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            List<Embedding> embeddings = new ArrayList<>();
            for (int index = 0; index < request.getInstructions().size(); index++) {
                embeddings.add(new Embedding(vectorize(request.getInstructions().get(index)), index));
            }
            return new EmbeddingResponse(embeddings);
        }

        @Override
        public float[] embed(Document document) {
            return vectorize(document.getText());
        }

        @Override
        public int dimensions() {
            return DIMENSIONS;
        }

        private float[] vectorize(String text) {
            float[] vector = new float[DIMENSIONS];
            String normalized = "  " + text.toLowerCase().replaceAll("[^a-z0-9]+", " ") + "  ";
            for (int index = 0; index <= normalized.length() - 3; index++) {
                String trigram = normalized.substring(index, index + 3);
                vector[Math.floorMod(trigram.hashCode(), DIMENSIONS)] += 1.0f;
            }
            return vector;
        }
    }
}
