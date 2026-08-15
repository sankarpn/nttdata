package com.nttdata.documentqa.service;

import com.nttdata.documentqa.model.DocumentChunk;
import com.nttdata.documentqa.model.RetrievedChunk;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryDocumentChunkRepository implements DocumentChunkRepository {
    static final String DOCUMENT_ID = "documentId";
    static final String FILENAME = "filename";
    static final String PAGE_NUMBER = "pageNumber";
    static final String LINE_START = "lineStart";
    static final String LINE_END = "lineEnd";
    static final String CHUNK_INDEX = "chunkIndex";

    private final VectorStore vectorStore;
    private final Map<String, DocumentChunk> chunks = new ConcurrentHashMap<>();

    public InMemoryDocumentChunkRepository(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void saveAll(List<DocumentChunk> chunks) {
        List<Document> documents = chunks.stream().map(this::toDocument).toList();
        vectorStore.add(documents);
        chunks.forEach(chunk -> this.chunks.put(chunk.chunkId(), chunk));
    }

    @Override
    public List<DocumentChunk> findAll() {
        return List.copyOf(chunks.values());
    }

    @Override
    public List<RetrievedChunk> similaritySearch(String question, int topK, double minimumScore) {
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(topK)
                .similarityThreshold(minimumScore)
                .build();
        return vectorStore.similaritySearch(request).stream()
                .map(this::toRetrievedChunk)
                .toList();
    }

    @Override
    public boolean isEmpty() {
        return chunks.isEmpty();
    }

    private Document toDocument(DocumentChunk chunk) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(DOCUMENT_ID, chunk.documentId());
        metadata.put(FILENAME, chunk.filename());
        metadata.put(CHUNK_INDEX, chunk.chunkIndex());
        putIfPresent(metadata, PAGE_NUMBER, chunk.pageNumber());
        putIfPresent(metadata, LINE_START, chunk.lineStart());
        putIfPresent(metadata, LINE_END, chunk.lineEnd());
        return new Document(chunk.chunkId(), chunk.text(), metadata);
    }

    private RetrievedChunk toRetrievedChunk(Document document) {
        DocumentChunk chunk = chunks.get(document.getId());
        if (chunk == null) {
            throw new IllegalStateException("Vector store returned unknown chunk " + document.getId());
        }
        return new RetrievedChunk(chunk, document.getScore() == null ? 0.0 : document.getScore());
    }

    private void putIfPresent(Map<String, Object> metadata, String key, Integer value) {
        if (value != null) {
            metadata.put(key, value);
        }
    }
}
