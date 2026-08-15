package com.nttdata.documentqa.service;

import com.nttdata.documentqa.model.DocumentChunk;
import com.nttdata.documentqa.model.RetrievedChunk;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InMemoryDocumentChunkRepositoryTest {

    @Test
    void storesCitationMetadataAndMapsSearchScoresBackToTheOriginalChunk() {
        VectorStore vectorStore = mock(VectorStore.class);
        InMemoryDocumentChunkRepository repository = new InMemoryDocumentChunkRepository(vectorStore);
        DocumentChunk chunk = new DocumentChunk(
                "chunk-1", "doc-1", "policy.pdf", 7, null, null, 3, "Cancellation terms");

        repository.saveAll(List.of(chunk));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> documents = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(documents.capture());
        Document stored = documents.getValue().getFirst();
        assertThat(stored.getId()).isEqualTo("chunk-1");
        assertThat(stored.getMetadata())
                .containsEntry("documentId", "doc-1")
                .containsEntry("filename", "policy.pdf")
                .containsEntry("pageNumber", 7)
                .containsEntry("chunkIndex", 3);

        Document match = mock(Document.class);
        when(match.getId()).thenReturn("chunk-1");
        when(match.getScore()).thenReturn(0.88);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(match));

        List<RetrievedChunk> results = repository.similaritySearch("Can I cancel?", 5, 0.70);

        assertThat(results).containsExactly(new RetrievedChunk(chunk, 0.88));
    }
}
