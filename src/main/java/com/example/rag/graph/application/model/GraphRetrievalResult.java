package com.example.rag.graph.application.model;

import java.util.List;

import org.springframework.ai.document.Document;

public record GraphRetrievalResult(
        String questionId,
        String question,
        List<String> graphEntities,
        String graphQueryType,
        List<String> graphRelationships,
        List<String> relatedDocumentIds,
        List<Document> vectorDocuments,
        List<Document> graphDocuments,
        List<Document> mergedDocuments,
        Status status
) {

    public GraphRetrievalResult {
        // 검색 결과 목록 불변 처리
        graphEntities = List.copyOf(graphEntities);
        graphRelationships = List.copyOf(graphRelationships);
        relatedDocumentIds = List.copyOf(relatedDocumentIds);
        vectorDocuments = List.copyOf(vectorDocuments);
        graphDocuments = List.copyOf(graphDocuments);
        mergedDocuments = List.copyOf(mergedDocuments);
    }

    public enum Status {
        SUCCESS,
        GRAPH_PATH_NOT_FOUND,
        GRAPH_DOCUMENT_SEARCH_EMPTY
    }
}