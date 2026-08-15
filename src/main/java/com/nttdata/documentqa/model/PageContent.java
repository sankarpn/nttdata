package com.nttdata.documentqa.model;

public record PageContent(Integer pageNumber, Integer lineStart, Integer lineEnd, String text) {

    public static PageContent pdfPage(int pageNumber, String text) {
        return new PageContent(pageNumber, null, null, text);
    }

    public static PageContent textLines(int lineStart, int lineEnd, String text) {
        return new PageContent(null, lineStart, lineEnd, text);
    }
}
