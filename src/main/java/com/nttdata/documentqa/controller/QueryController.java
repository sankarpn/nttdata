package com.nttdata.documentqa.controller;

import com.nttdata.documentqa.model.QueryRequest;
import com.nttdata.documentqa.model.QueryResponse;
import com.nttdata.documentqa.service.RagService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/query")
public class QueryController {
    private final RagService ragService;

    public QueryController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<QueryResponse> query(@Valid @RequestBody QueryRequest request) {
        return ragService.answer(request.question());
    }
}
