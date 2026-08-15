package com.nttdata.documentqa.service;

import com.nttdata.documentqa.model.DocumentChunk;
import com.nttdata.documentqa.model.RetrievedChunk;

import java.util.List;

public interface DocumentChunkRepository {
    void saveAll(List<DocumentChunk> chunks);
    List<DocumentChunk> findAll();
    List<RetrievedChunk> similaritySearch(String question, int topK, double minimumScore);
    boolean isEmpty();
}
