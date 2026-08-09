package com.example.rag.graph.application.model;

import java.util.List;

public record GraphQuestion(
        String questionId,
        String queryType,
        List<String> entityNames
) {

    public GraphQuestion {
        // 엔티티 목록 불변 처리
        entityNames = entityNames == null
                ? null
                : List.copyOf(entityNames);
    }
}