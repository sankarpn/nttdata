package com.nttdata.documentqa.service;

import com.nttdata.documentqa.config.RetrievalProperties;
import com.nttdata.documentqa.exception.EmptyLibraryException;
import com.nttdata.documentqa.model.DocumentChunk;
import com.nttdata.documentqa.model.RetrievalResult;
import com.nttdata.documentqa.model.RetrievedChunk;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RetrievalServiceTest {
    private final DocumentChunkRepository repository = mock(DocumentChunkRepository.class);
    private final RetrievalService service = new RetrievalService(repository, new RetrievalProperties(5, 0.70));

    @Test
    void rejectsQueriesAgainstAnEmptyLibraryBeforeEmbeddingTheQuestion() {
        when(repository.isEmpty()).thenReturn(true);

        StepVerifier.create(service.retrieve("What is covered?"))
                .expectError(EmptyLibraryException.class)
                .verify();
    }

    @Test
    void returnsNoMatchAndNoChunksWhenNothingClearsTheThreshold() {
        when(repository.isEmpty()).thenReturn(false);
        when(repository.similaritySearch("unrelated question", 5, 0.70)).thenReturn(List.of());

        StepVerifier.create(service.retrieve("unrelated question"))
                .assertNext(result -> {
                    assertThat(result.status()).isEqualTo(RetrievalResult.Status.NO_MATCH);
                    assertThat(result.chunks()).isEmpty();
                })
                .verifyComplete();
        verify(repository).similaritySearch("unrelated question", 5, 0.70);
    }

    @Test
    void returnsRetrievedChunksWithTheirOriginalCitationMetadata() {
        DocumentChunk chunk = new DocumentChunk(
                "chunk-1", "doc-1", "policy.pdf", 7, null, null, 0, "Cancellation terms");
        when(repository.isEmpty()).thenReturn(false);
        when(repository.similaritySearch("Can I cancel?", 5, 0.70))
                .thenReturn(List.of(new RetrievedChunk(chunk, 0.91)));

        StepVerifier.create(service.retrieve("Can I cancel?"))
                .assertNext(result -> {
                    assertThat(result.status()).isEqualTo(RetrievalResult.Status.MATCH);
                    assertThat(result.chunks()).containsExactly(new RetrievedChunk(chunk, 0.91));
                })
                .verifyComplete();
    }
}
