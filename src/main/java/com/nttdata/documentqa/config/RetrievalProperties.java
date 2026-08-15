package com.nttdata.documentqa.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.retrieval")
public record RetrievalProperties(int topK, double minimumScore) {

    public RetrievalProperties {
        if (topK <= 0) {
            throw new IllegalArgumentException("app.retrieval.top-k must be positive");
        }
        if (minimumScore < 0.0 || minimumScore > 1.0) {
            throw new IllegalArgumentException("app.retrieval.minimum-score must be between 0 and 1");
        }
    }
}
