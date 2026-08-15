package com.nttdata.documentqa.service;

public interface AnswerGenerator {
    String generate(String systemPrompt, String userPrompt);
}
