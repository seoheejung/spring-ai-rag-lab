package com.example.rag.evaluation.application.model;

public record RetrievalMetrics(
        Integer hitAtK,
        Double recallAtK,
        Double precisionAtK,
        Double reciprocalRank
) {

    public static RetrievalMetrics notApplicable() {
        // 검색 지표 평가 제외 결과 생성
        return new RetrievalMetrics(
                null,
                null,
                null,
                null
        );
    }
}