package com.example.rag.graph.config;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag.graph")
public record GraphRagProperties(
        boolean enabled,
        Path questionsPath,
        Path graphQuestionsPath,
        Path resultsPath,
        int topK,
        double similarityThreshold
) {
}