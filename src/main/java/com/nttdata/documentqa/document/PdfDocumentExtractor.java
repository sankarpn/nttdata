package com.nttdata.documentqa.document;

import com.nttdata.documentqa.exception.InvalidDocumentException;
import com.nttdata.documentqa.model.PageContent;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class PdfDocumentExtractor implements DocumentExtractor {

    @Override
    public DocumentType supports() {
        return DocumentType.PDF;
    }

    @Override
    public List<PageContent> extract(byte[] content) {
        try (PDDocument document = Loader.loadPDF(content)) {
            PDFTextStripper stripper = new PDFTextStripper();
            List<PageContent> pages = new ArrayList<>(document.getNumberOfPages());
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                pages.add(PageContent.pdfPage(page, stripper.getText(document).strip()));
            }
            return pages;
        } catch (IOException exception) {
            throw new InvalidDocumentException("The uploaded PDF could not be read.", exception);
        }
    }
}
