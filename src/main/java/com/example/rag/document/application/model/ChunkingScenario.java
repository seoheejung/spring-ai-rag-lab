package com.example.rag.document.application.model;

import java.util.List;

public record ChunkingScenario(
        String code,
        int chunkSize,
        boolean keepSeparator,
        String description
) {
    public static List<ChunkingScenario> comparisonCases() {
        return List.of(
                new ChunkingScenario(
                        "A",
                        300,
                        true,
                        "Chunk Size 300"
                ),
                new ChunkingScenario(
                        "B",
                        800,
                        true,
                        "Chunk Size 800"
                ),
                new ChunkingScenario(
                        "C",
                        1200,
                        true,
                        "Chunk Size 1200"
                ),
                new ChunkingScenario(
                        "D",
                        800,
                        false,
                        "keepSeparator false"
                ),
                new ChunkingScenario(
                        "E",
                        800,
                        true,
                        "keepSeparator true"
                )
        );
    }
}