package com.nttdata.documentqa.document;

import com.nttdata.documentqa.exception.UnsupportedDocumentException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentValidatorTest {
    private final DocumentValidator validator = new DocumentValidator();

    @Test
    void acceptsPdfAndTextDocuments() {
        assertThat(validator.validate(file("policy.pdf", MediaType.APPLICATION_PDF))).isEqualTo(DocumentType.PDF);
        assertThat(validator.validate(file("notes.txt", MediaType.TEXT_PLAIN))).isEqualTo(DocumentType.TEXT);
    }

    @Test
    void rejectsUnsupportedOrMismatchedTypes() {
        assertThatThrownBy(() -> validator.validate(file("policy.docx", MediaType.APPLICATION_OCTET_STREAM)))
                .isInstanceOf(UnsupportedDocumentException.class);
        assertThatThrownBy(() -> validator.validate(file("policy.pdf", MediaType.TEXT_PLAIN)))
                .isInstanceOf(UnsupportedDocumentException.class);
    }

    private FilePart file(String filename, MediaType mediaType) {
        FilePart file = mock(FilePart.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        when(file.filename()).thenReturn(filename);
        when(file.headers()).thenReturn(headers);
        return file;
    }
}
