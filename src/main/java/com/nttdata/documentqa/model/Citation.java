package com.nttdata.documentqa.model;

public record Citation(
        String documentId,
        String filename,
        Integer page,
        Integer lineStart,
        Integer lineEnd,
        String chunkId) {

    public static Citation from(DocumentChunk chunk) {
        return new Citation(chunk.documentId(), chunk.filename(), chunk.pageNumber(),
                chunk.lineStart(), chunk.lineEnd(), chunk.chunkId());
    }
}
