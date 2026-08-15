package com.nttdata.documentqa.model;

import java.util.List;

public record QueryResponse(String answer, List<Citation> citations) {
    public QueryResponse {
        citations = List.copyOf(citations);
    }
}
