package com.nttdata.documentqa.model;

import jakarta.validation.constraints.NotBlank;

public record QueryRequest(@NotBlank(message = "Question must not be blank.") String question) {
}
