package com.example.rag.graph.application;

import java.util.List;

import org.springframework.ai.document.Document;

public interface RelatedDocumentSearch {

    List<Document> search(
            String question,
            List<String> documentIds,
            int topK,
            double similarityThreshold
    );
}