package com.nttdata.documentqa.document;

import com.nttdata.documentqa.model.PageContent;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class TextDocumentExtractor implements DocumentExtractor {

    @Override
    public DocumentType supports() {
        return DocumentType.TEXT;
    }

    @Override
    public List<PageContent> extract(byte[] content) {
        String text = new String(content, StandardCharsets.UTF_8);
        int lineCount = text.isEmpty() ? 0 : text.split("\\R", -1).length;
        return List.of(PageContent.textLines(1, lineCount, text.strip()));
    }
}
