package com.nttdata.documentqa.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class SpringAiAnswerGenerator implements AnswerGenerator {
    private final ChatClient chatClient;

    public SpringAiAnswerGenerator(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
    }
}
