package com.nttdata.documentqa.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties("app.documents")
public record DocumentProperties(DataSize maxSize, int chunkSize, int chunkOverlap) {

    public DocumentProperties {
        if (maxSize == null || maxSize.toBytes() <= 0) {
            throw new IllegalArgumentException("app.documents.max-size must be positive");
        }
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("app.documents.chunk-size must be positive");
        }
        if (chunkOverlap < 0 || chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException("app.documents.chunk-overlap must be between 0 and chunk-size - 1");
        }
    }
}
