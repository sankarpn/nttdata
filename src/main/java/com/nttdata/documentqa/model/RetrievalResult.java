package com.nttdata.documentqa.model;

import java.util.List;

public record RetrievalResult(Status status, List<RetrievedChunk> chunks) {
    public enum Status {
        MATCH,
        NO_MATCH
    }

    public RetrievalResult {
        chunks = List.copyOf(chunks);
    }

    public static RetrievalResult match(List<RetrievedChunk> chunks) {
        return new RetrievalResult(Status.MATCH, chunks);
    }

    public static RetrievalResult noMatch() {
        return new RetrievalResult(Status.NO_MATCH, List.of());
    }
}
