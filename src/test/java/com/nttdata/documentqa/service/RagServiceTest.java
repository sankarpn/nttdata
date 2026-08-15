package com.nttdata.documentqa.service;

import com.nttdata.documentqa.model.Citation;
import com.nttdata.documentqa.model.DocumentChunk;
import com.nttdata.documentqa.model.RetrievalResult;
import com.nttdata.documentqa.model.RetrievedChunk;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RagServiceTest {
    private final RetrievalService retrievalService = mock(RetrievalService.class);
    private final AnswerGenerator answerGenerator = mock(AnswerGenerator.class);
    private final RagService service = new RagService(
            retrievalService, new GroundingPromptFactory(), answerGenerator);

    @Test
    void weakMatchReturnsHonestAnswerWithoutCallingTheLlm() {
        when(retrievalService.retrieve("unrelated question"))
                .thenReturn(Mono.just(RetrievalResult.noMatch()));

        StepVerifier.create(service.answer("unrelated question"))
                .assertNext(response -> {
                    assertThat(response.answer()).isEqualTo(RagService.NO_MATCH_ANSWER);
                    assertThat(response.citations()).isEmpty();
                })
                .verifyComplete();
        verify(answerGenerator, never()).generate(anyString(), anyString());
    }

    @Test
    void strongMatchGroundsGenerationAndBuildsCitationsFromRetrievedChunks() {
        DocumentChunk chunk = new DocumentChunk(
                "chunk-1", "doc-1", "policy.pdf", 7, null, null, 0, "Cancellation is allowed within 30 days.");
        when(retrievalService.retrieve("Can I cancel within 30 days?"))
                .thenReturn(Mono.just(RetrievalResult.match(List.of(new RetrievedChunk(chunk, 0.93)))));
        when(answerGenerator.generate(anyString(), anyString()))
                .thenReturn("The policy permits cancellation within 30 days.");

        StepVerifier.create(service.answer("Can I cancel within 30 days?"))
                .assertNext(response -> {
                    assertThat(response.answer()).isEqualTo("The policy permits cancellation within 30 days.");
                    assertThat(response.citations()).containsExactly(
                            new Citation("doc-1", "policy.pdf", 7, null, null, "chunk-1"));
                })
                .verifyComplete();

        ArgumentCaptor<String> systemPrompt = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> userPrompt = ArgumentCaptor.forClass(String.class);
        verify(answerGenerator).generate(systemPrompt.capture(), userPrompt.capture());
        assertThat(systemPrompt.getValue()).contains("ONLY from the supplied context");
        assertThat(userPrompt.getValue())
                .contains("Cancellation is allowed within 30 days.")
                .contains("page=7")
                .contains("Can I cancel within 30 days?");
    }
}
