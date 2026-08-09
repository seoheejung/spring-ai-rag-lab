package com.example.rag.graph.application.model;

import java.util.List;

public record GraphPathResult(
        List<String> entities,
        List<String> relationships,
        List<String> documentIds
) {

    public GraphPathResult {
        // Graph 결과 불변 처리
        entities = List.copyOf(entities);
        relationships = List.copyOf(relationships);
        documentIds = List.copyOf(documentIds);
    }
}