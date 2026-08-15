package com.nttdata.documentqa.exception;

public class EmptyDocumentException extends RuntimeException {
    public EmptyDocumentException(String message) {
        super(message);
    }
}
