package com.nttdata.documentqa.model;

public record UploadResponse(String documentId, String filename, int chunksCreated, String status) {
}
