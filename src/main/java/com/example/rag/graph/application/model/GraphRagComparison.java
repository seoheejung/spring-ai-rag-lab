package com.example.rag.graph.application.model;

import java.util.List;

import com.example.rag.evaluation.application.model.RetrievalMetrics;
import com.example.rag.evaluation.application.model.RetrievedChunk;

public record GraphRagComparison(
        int topK,
        double similarityThreshold,
        List<CaseResult> results
) {

    public GraphRagComparison {
        // 비교 결과 목록 불변 처리
        results = List.copyOf(results);
    }

    public record CaseResult(
            String questionId,
            String question,
            String questionType,
            List<String> expectedDocumentIds,
            List<String> expectedChunkIds,
            List<String> graphEntities,
            String graphQueryType,
            List<String> graphRelationships,
            int graphHopCount,
            List<String> relatedDocumentIds,
            List<RetrievedChunk> generalRetrievedChunks,
            List<RetrievedChunk> graphDocumentRetrievedChunks,
            List<RetrievedChunk> mergedRetrievedChunks,
            RetrievalMetrics generalRetrievalMetrics,
            RetrievalMetrics graphRetrievalMetrics,
            String generalAnswer,
            String graphAnswer,
            GraphRetrievalResult.Status status,
            String failureReason
    ) {

        public CaseResult {
            // 결과 목록 불변 처리
            expectedDocumentIds = List.copyOf(expectedDocumentIds);
            expectedChunkIds = List.copyOf(expectedChunkIds);
            graphEntities = List.copyOf(graphEntities);
            graphRelationships = List.copyOf(graphRelationships);
            relatedDocumentIds = List.copyOf(relatedDocumentIds);
            generalRetrievedChunks = List.copyOf(generalRetrievedChunks);
            graphDocumentRetrievedChunks = List.copyOf(graphDocumentRetrievedChunks);
            mergedRetrievedChunks = List.copyOf(mergedRetrievedChunks);
        }
    }
}