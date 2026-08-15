package com.nttdata.documentqa.service;

import com.nttdata.documentqa.config.RetrievalProperties;
import com.nttdata.documentqa.exception.EmptyLibraryException;
import com.nttdata.documentqa.model.RetrievalResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class RetrievalService {
    private final DocumentChunkRepository repository;
    private final RetrievalProperties properties;

    public RetrievalService(DocumentChunkRepository repository, RetrievalProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    public Mono<RetrievalResult> retrieve(String question) {
        if (question == null || question.isBlank()) {
            return Mono.error(new IllegalArgumentException("Question must not be blank."));
        }
        if (repository.isEmpty()) {
            return Mono.error(new EmptyLibraryException());
        }

        return Mono.fromCallable(() -> repository.similaritySearch(
                        question, properties.topK(), properties.minimumScore()))
                .subscribeOn(Schedulers.boundedElastic())
                .map(chunks -> chunks.isEmpty() ? RetrievalResult.noMatch() : RetrievalResult.match(chunks));
    }
}
