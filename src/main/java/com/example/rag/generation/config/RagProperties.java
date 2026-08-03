package com.example.rag.generation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag.generation")
public record RagProperties(
        boolean enabled,
        String question,
        int topK,
        double similarityThreshold
) {
}