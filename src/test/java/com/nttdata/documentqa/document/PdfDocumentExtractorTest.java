package com.nttdata.documentqa.document;

import com.nttdata.documentqa.model.PageContent;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PdfDocumentExtractorTest {
    private final PdfDocumentExtractor extractor = new PdfDocumentExtractor();

    @Test
    void extractsTextOnePageAtATimeWithActualPageNumbers() throws IOException {
        List<PageContent> pages = extractor.extract(twoPagePdf());

        assertThat(pages).hasSize(2);
        assertThat(pages.get(0).pageNumber()).isEqualTo(1);
        assertThat(pages.get(0).text()).contains("First page policy terms");
        assertThat(pages.get(1).pageNumber()).isEqualTo(2);
        assertThat(pages.get(1).text()).contains("Second page cancellation terms");
    }

    private byte[] twoPagePdf() throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            addPage(document, "First page policy terms");
            addPage(document, "Second page cancellation terms");
            document.save(output);
            return output.toByteArray();
        }
    }

    private void addPage(PDDocument document, String text) throws IOException {
        PDPage page = new PDPage();
        document.addPage(page);
        try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
            stream.beginText();
            stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            stream.newLineAtOffset(72, 720);
            stream.showText(text);
            stream.endText();
        }
    }
}
