package com.nttdata.documentqa.document;

import com.nttdata.documentqa.model.PageContent;

import java.util.List;

public interface DocumentExtractor {
    DocumentType supports();
    List<PageContent> extract(byte[] content);
}
