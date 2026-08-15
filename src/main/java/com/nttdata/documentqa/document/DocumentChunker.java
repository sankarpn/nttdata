package com.nttdata.documentqa.document;

import com.nttdata.documentqa.config.DocumentProperties;
import com.nttdata.documentqa.model.DocumentChunk;
import com.nttdata.documentqa.model.PageContent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class DocumentChunker {
    private final DocumentProperties properties;

    public DocumentChunker(DocumentProperties properties) {
        this.properties = properties;
    }

    public List<DocumentChunk> chunk(String documentId, String filename, List<PageContent> sources) {
        List<DocumentChunk> chunks = new ArrayList<>();
        int chunkIndex = 0;
        int step = properties.chunkSize() - properties.chunkOverlap();

        for (PageContent source : sources) {
            if (source.text() == null || source.text().isBlank()) {
                continue;
            }
            String[] words = source.text().trim().split("\\s+");
            for (int start = 0; start < words.length; start += step) {
                int end = Math.min(start + properties.chunkSize(), words.length);
                chunks.add(new DocumentChunk(
                        UUID.randomUUID().toString(), documentId, filename,
                        source.pageNumber(), source.lineStart(), source.lineEnd(),
                        chunkIndex++, String.join(" ", java.util.Arrays.copyOfRange(words, start, end))));
                if (end == words.length) {
                    break;
                }
            }
        }
        return chunks;
    }
}
