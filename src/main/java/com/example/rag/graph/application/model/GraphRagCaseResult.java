package com.example.rag.graph.application.model;

public record GraphRagCaseResult(
        String questionId,
        String question,
        GraphRetrievalResult retrievalResult,
        String generalAnswer,
        String graphAnswer
) {
}