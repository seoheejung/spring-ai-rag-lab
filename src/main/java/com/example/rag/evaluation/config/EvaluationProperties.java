package com.example.rag.evaluation.config;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag.evaluation")
public record EvaluationProperties(
        boolean enabled,
        Path questionsPath,
        Path resultsPath,
        int topK,
        double similarityThreshold
) {
}