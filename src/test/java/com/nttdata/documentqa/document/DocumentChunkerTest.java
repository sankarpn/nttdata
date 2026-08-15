package com.nttdata.documentqa.document;

import com.nttdata.documentqa.config.DocumentProperties;
import com.nttdata.documentqa.model.DocumentChunk;
import com.nttdata.documentqa.model.PageContent;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentChunkerTest {

    @Test
    void preservesDocumentAndPageMetadataAcrossOverlappingChunks() {
        DocumentChunker chunker = new DocumentChunker(new DocumentProperties(DataSize.ofMegabytes(10), 4, 1));

        List<DocumentChunk> chunks = chunker.chunk("doc-1", "policy.pdf",
                List.of(PageContent.pdfPage(7, "one two three four five six seven")));

        assertThat(chunks).hasSize(2);
        assertThat(chunks).allSatisfy(chunk -> {
            assertThat(chunk.documentId()).isEqualTo("doc-1");
            assertThat(chunk.filename()).isEqualTo("policy.pdf");
            assertThat(chunk.pageNumber()).isEqualTo(7);
            assertThat(chunk.chunkId()).isNotBlank();
        });
        assertThat(chunks).extracting(DocumentChunk::chunkIndex).containsExactly(0, 1);
        assertThat(chunks).extracting(DocumentChunk::text)
                .containsExactly("one two three four", "four five six seven");
    }
}
