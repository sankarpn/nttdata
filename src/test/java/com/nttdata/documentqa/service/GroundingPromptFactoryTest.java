package com.nttdata.documentqa.service;

import com.nttdata.documentqa.model.DocumentChunk;
import com.nttdata.documentqa.model.RetrievedChunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GroundingPromptFactoryTest {
    private final GroundingPromptFactory factory = new GroundingPromptFactory();

    @Test
    void labelsRetrievedContextWithBackendSourceMetadata() {
        DocumentChunk pdf = new DocumentChunk(
                "chunk-pdf", "doc-1", "policy.pdf", 7, null, null, 0, "Thirty day cancellation.");
        DocumentChunk text = new DocumentChunk(
                "chunk-txt", "doc-2", "notes.txt", null, 1, 12, 1, "Plain text terms.");

        String prompt = factory.userPrompt("Can I cancel?",
                List.of(new RetrievedChunk(pdf, 0.91), new RetrievedChunk(text, 0.84)));

        assertThat(prompt)
                .contains("[SOURCE 1 | filename=policy.pdf | page=7 | chunkId=chunk-pdf]")
                .contains("[SOURCE 2 | filename=notes.txt | lines=1-12 | chunkId=chunk-txt]")
                .contains("Thirty day cancellation.")
                .endsWith("USER QUESTION\nCan I cancel?");
        assertThat(factory.systemPrompt())
                .contains("ONLY from the supplied context")
                .contains("Do not invent, infer, or include citations");
    }
}
