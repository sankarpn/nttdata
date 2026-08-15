package com.nttdata.documentqa.service;

import com.nttdata.documentqa.config.DocumentProperties;
import com.nttdata.documentqa.document.DocumentChunker;
import com.nttdata.documentqa.document.DocumentExtractor;
import com.nttdata.documentqa.document.DocumentType;
import com.nttdata.documentqa.document.DocumentValidator;
import com.nttdata.documentqa.exception.DocumentTooLargeException;
import com.nttdata.documentqa.exception.EmptyDocumentException;
import com.nttdata.documentqa.model.DocumentChunk;
import com.nttdata.documentqa.model.PageContent;
import com.nttdata.documentqa.model.UploadResponse;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayOutputStream;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DocumentIngestionService {
    private final DocumentProperties properties;
    private final DocumentValidator validator;
    private final Map<DocumentType, DocumentExtractor> extractors;
    private final DocumentChunker chunker;
    private final DocumentChunkRepository repository;

    public DocumentIngestionService(DocumentProperties properties, DocumentValidator validator,
                                    List<DocumentExtractor> extractors, DocumentChunker chunker,
                                    DocumentChunkRepository repository) {
        this.properties = properties;
        this.validator = validator;
        this.extractors = new EnumMap<>(DocumentType.class);
        extractors.forEach(extractor -> this.extractors.put(extractor.supports(), extractor));
        this.chunker = chunker;
        this.repository = repository;
    }

    public Mono<UploadResponse> ingest(FilePart file) {
        DocumentType type = validator.validate(file);
        return readWithLimit(file)
                .publishOn(Schedulers.boundedElastic())
                .map(bytes -> extractAndStore(file.filename(), type, bytes));
    }

    private Mono<byte[]> readWithLimit(FilePart file) {
        long maximum = properties.maxSize().toBytes();
        return file.content().reduce(new ByteArrayOutputStream(), (output, buffer) -> {
            try {
                int readable = buffer.readableByteCount();
                if ((long) output.size() + readable > maximum) {
                    throw new DocumentTooLargeException(
                            "Maximum supported document size is " + properties.maxSize().toMegabytes() + " MB.");
                }
                byte[] bytes = new byte[readable];
                buffer.read(bytes);
                output.writeBytes(bytes);
                return output;
            } finally {
                DataBufferUtils.release(buffer);
            }
        }).map(ByteArrayOutputStream::toByteArray);
    }

    private UploadResponse extractAndStore(String filename, DocumentType type, byte[] bytes) {
        List<PageContent> pages = extractors.get(type).extract(bytes);
        String documentId = UUID.randomUUID().toString();
        List<DocumentChunk> chunks = chunker.chunk(documentId, filename, pages);
        if (chunks.isEmpty()) {
            throw new EmptyDocumentException("No usable text could be extracted from the document.");
        }
        repository.saveAll(chunks);
        return new UploadResponse(documentId, filename, chunks.size(), "INGESTED");
    }
}
