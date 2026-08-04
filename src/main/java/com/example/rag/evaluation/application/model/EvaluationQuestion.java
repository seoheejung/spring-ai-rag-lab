package com.example.rag.evaluation.application.model;

import java.util.List;

public record EvaluationQuestion(
        String id,
        String question,
        String type,
        List<String> expectedDocumentIds,
        List<String> expectedChunkIds,
        boolean answerable
) {

    public EvaluationQuestion {
        // 외부 변경 방지를 위한 불변 목록 생성
        expectedDocumentIds = List.copyOf(expectedDocumentIds);
        expectedChunkIds = List.copyOf(expectedChunkIds);
    }
}