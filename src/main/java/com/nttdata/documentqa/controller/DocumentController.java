package com.nttdata.documentqa.controller;

import com.nttdata.documentqa.model.UploadResponse;
import com.nttdata.documentqa.service.DocumentIngestionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final DocumentIngestionService ingestionService;

    public DocumentController(DocumentIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<UploadResponse> upload(@RequestPart("file") FilePart file) {
        return ingestionService.ingest(file);
    }
}
