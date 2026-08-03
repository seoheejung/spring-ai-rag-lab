package com.example.rag.generation.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagChatConfig {

    @Bean
    ChatClient chatClient(
            ChatClient.Builder chatClientBuilder
    ) {
        // ChatClient 생성
        return chatClientBuilder.build();
    }
}