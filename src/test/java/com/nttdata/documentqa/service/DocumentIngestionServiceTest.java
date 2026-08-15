package com.nttdata.documentqa.service;

import com.nttdata.documentqa.config.DocumentProperties;
import com.nttdata.documentqa.document.DocumentChunker;
import com.nttdata.documentqa.document.DocumentValidator;
import com.nttdata.documentqa.document.PdfDocumentExtractor;
import com.nttdata.documentqa.document.TextDocumentExtractor;
import com.nttdata.documentqa.exception.DocumentTooLargeException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.util.unit.DataSize;
import org.springframework.ai.vectorstore.VectorStore;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentIngestionServiceTest {

    @Test
    void rejectsAFileAsSoonAsItExceedsTheConfiguredMaximum() {
        DocumentProperties properties = new DocumentProperties(DataSize.ofBytes(5), 10, 2);
        DocumentIngestionService service = new DocumentIngestionService(
                properties, new DocumentValidator(),
                java.util.List.of(new PdfDocumentExtractor(), new TextDocumentExtractor()),
                new DocumentChunker(properties), new InMemoryDocumentChunkRepository(mock(VectorStore.class)));
        FilePart file = textFile("123456");

        StepVerifier.create(service.ingest(file))
                .expectError(DocumentTooLargeException.class)
                .verify();
    }

    private FilePart textFile(String content) {
        FilePart file = mock(FilePart.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        when(file.filename()).thenReturn("sample.txt");
        when(file.headers()).thenReturn(headers);
        when(file.content()).thenReturn(Flux.just(
                DefaultDataBufferFactory.sharedInstance.wrap(content.getBytes(java.nio.charset.StandardCharsets.UTF_8))));
        return file;
    }
}
