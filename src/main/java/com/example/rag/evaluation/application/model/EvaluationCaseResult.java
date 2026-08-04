package com.example.rag.evaluation.application.model;

import java.util.List;

public record EvaluationCaseResult(
        String id,
        String question,
        String type,
        boolean answerable,
        List<String> expectedDocumentIds,
        List<String> expectedChunkIds,
        List<RetrievedChunk> retrievedChunks,
        RetrievalMetrics retrievalMetrics,
        String answer,
        Boolean answerCorrect,
        Boolean groundedness,
        Boolean relevance,
        Boolean completeness,
        Boolean citationAccuracy,
        Boolean unsupportedClaim,
        Boolean refusalAccuracy,
        String failureType,
        String notes
) {

    public EvaluationCaseResult {
        // 외부 변경 방지를 위한 불변 목록 생성
        expectedDocumentIds = List.copyOf(expectedDocumentIds);
        expectedChunkIds = List.copyOf(expectedChunkIds);
        retrievedChunks = List.copyOf(retrievedChunks);
    }

    public static EvaluationCaseResult unreviewed(
            EvaluationQuestion question,
            List<RetrievedChunk> retrievedChunks,
            RetrievalMetrics retrievalMetrics,
            String answer
    ) {
        // 수동 검토 전 평가 결과 생성
        return new EvaluationCaseResult(
                question.id(),
                question.question(),
                question.type(),
                question.answerable(),
                question.expectedDocumentIds(),
                question.expectedChunkIds(),
                retrievedChunks,
                retrievalMetrics,
                answer,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "UNREVIEWED",
                ""
        );
    }
}