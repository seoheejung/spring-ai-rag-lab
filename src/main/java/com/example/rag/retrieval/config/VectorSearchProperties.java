package com.example.rag.retrieval.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag.retrieval")
public record VectorSearchProperties(
        boolean enabled,
        boolean store,
        boolean search,
        String question,
        int topK,
        double similarityThreshold
) {
}