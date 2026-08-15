package com.nttdata.documentqa.model;

public record DocumentChunk(
        String chunkId,
        String documentId,
        String filename,
        Integer pageNumber,
        Integer lineStart,
        Integer lineEnd,
        int chunkIndex,
        String text) {
}
