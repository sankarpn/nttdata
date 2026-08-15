package com.nttdata.documentqa.exception;

public class EmptyLibraryException extends RuntimeException {
    public EmptyLibraryException() {
        super("Upload at least one document before asking a question.");
    }
}
