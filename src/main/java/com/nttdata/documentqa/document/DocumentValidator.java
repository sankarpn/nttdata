package com.nttdata.documentqa.document;

import com.nttdata.documentqa.exception.UnsupportedDocumentException;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class DocumentValidator {

    public DocumentType validate(FilePart file) {
        String filename = file.filename().toLowerCase(Locale.ROOT);
        MediaType contentType = file.headers().getContentType();

        if (filename.endsWith(".pdf") && isCompatible(contentType, MediaType.APPLICATION_PDF)) {
            return DocumentType.PDF;
        }
        if (filename.endsWith(".txt") && isCompatible(contentType, MediaType.TEXT_PLAIN)) {
            return DocumentType.TEXT;
        }
        throw new UnsupportedDocumentException("Only PDF and TXT documents are supported.");
    }

    private boolean isCompatible(MediaType actual, MediaType expected) {
        return actual == null || MediaType.APPLICATION_OCTET_STREAM.equals(actual) || expected.isCompatibleWith(actual);
    }
}
