package com.nttdata.documentqa.service;

import com.nttdata.documentqa.model.Citation;
import com.nttdata.documentqa.model.QueryResponse;
import com.nttdata.documentqa.model.RetrievalResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Service
public class RagService {
    static final String NO_MATCH_ANSWER =
            "I could not find sufficient information in the uploaded documents to answer this question.";

    private final RetrievalService retrievalService;
    private final GroundingPromptFactory promptFactory;
    private final AnswerGenerator answerGenerator;

    public RagService(RetrievalService retrievalService, GroundingPromptFactory promptFactory,
                      AnswerGenerator answerGenerator) {
        this.retrievalService = retrievalService;
        this.promptFactory = promptFactory;
        this.answerGenerator = answerGenerator;
    }

    public Mono<QueryResponse> answer(String question) {
        return retrievalService.retrieve(question).flatMap(result -> {
            if (result.status() == RetrievalResult.Status.NO_MATCH) {
                return Mono.just(new QueryResponse(NO_MATCH_ANSWER, List.of()));
            }

            String userPrompt = promptFactory.userPrompt(question, result.chunks());
            return Mono.fromCallable(() -> answerGenerator.generate(promptFactory.systemPrompt(), userPrompt))
                    .subscribeOn(Schedulers.boundedElastic())
                    .map(answer -> new QueryResponse(answer, result.chunks().stream()
                            .map(retrieved -> Citation.from(retrieved.chunk()))
                            .distinct()
                            .toList()));
        });
    }
}
