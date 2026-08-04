package com.example.rag.retrieval.config;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag.improvement")
public record SearchImprovementProperties(
        boolean enabled,
        Path questionsPath,
        Path baselineResultsPath,
        Path resultsPath,
        int keywordTopK,
        int vectorTopK,
        double similarityThreshold,
        int rrfK,
        int candidateCount,
        int finalTopK,
        boolean rerankingEnabled
) {
}