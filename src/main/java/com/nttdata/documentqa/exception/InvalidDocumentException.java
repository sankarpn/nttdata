package com.nttdata.documentqa.exception;

public class InvalidDocumentException extends RuntimeException {
    public InvalidDocumentException(String message, Throwable cause) {
        super(message, cause);
    }
}
