package com.nttdata.documentqa.service;

import com.nttdata.documentqa.model.DocumentChunk;
import com.nttdata.documentqa.model.RetrievedChunk;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GroundingPromptFactory {
    static final String SYSTEM_PROMPT = """
            You are a document question-answering assistant.

            Answer the user's question ONLY from the supplied context.
            If the context does not contain enough information to answer the question, state that the uploaded documents do not contain enough information.
            Do not use outside knowledge.
            Do not invent facts.
            Do not invent, infer, or include citations; the backend adds citations separately.
            """;

    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    public String userPrompt(String question, List<RetrievedChunk> retrievedChunks) {
        StringBuilder prompt = new StringBuilder("SUPPLIED CONTEXT\n\n");
        for (int index = 0; index < retrievedChunks.size(); index++) {
            DocumentChunk chunk = retrievedChunks.get(index).chunk();
            prompt.append(sourceLabel(index + 1, chunk))
                    .append('\n')
                    .append(chunk.text())
                    .append("\n\n");
        }
        return prompt.append("USER QUESTION\n").append(question).toString();
    }

    private String sourceLabel(int number, DocumentChunk chunk) {
        String location = chunk.pageNumber() != null
                ? "page=" + chunk.pageNumber()
                : "lines=" + chunk.lineStart() + "-" + chunk.lineEnd();
        return "[SOURCE " + number + " | filename=" + chunk.filename() + " | " + location
                + " | chunkId=" + chunk.chunkId() + "]";
    }
}
