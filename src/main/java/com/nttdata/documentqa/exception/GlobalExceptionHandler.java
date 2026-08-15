package com.nttdata.documentqa.exception;

import com.nttdata.documentqa.model.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DocumentTooLargeException.class)
    ResponseEntity<ApiError> documentTooLarge(DocumentTooLargeException exception) {
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
                .body(new ApiError("DOCUMENT_TOO_LARGE", exception.getMessage()));
    }

    @ExceptionHandler(UnsupportedDocumentException.class)
    ResponseEntity<ApiError> unsupportedDocument(UnsupportedDocumentException exception) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(new ApiError("UNSUPPORTED_DOCUMENT_TYPE", exception.getMessage()));
    }

    @ExceptionHandler(EmptyDocumentException.class)
    ResponseEntity<ApiError> emptyDocument(EmptyDocumentException exception) {
        return ResponseEntity.badRequest().body(new ApiError("EMPTY_DOCUMENT", exception.getMessage()));
    }

    @ExceptionHandler(InvalidDocumentException.class)
    ResponseEntity<ApiError> invalidDocument(InvalidDocumentException exception) {
        return ResponseEntity.badRequest().body(new ApiError("INVALID_DOCUMENT", exception.getMessage()));
    }

    @ExceptionHandler(EmptyLibraryException.class)
    ResponseEntity<ApiError> emptyLibrary(EmptyLibraryException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("EMPTY_DOCUMENT_LIBRARY", exception.getMessage()));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    ResponseEntity<ApiError> invalidRequest(WebExchangeBindException exception) {
        String message = exception.getFieldErrors().isEmpty()
                ? "The request is invalid."
                : exception.getFieldErrors().getFirst().getDefaultMessage();
        return ResponseEntity.badRequest().body(new ApiError("INVALID_REQUEST", message));
    }
}
