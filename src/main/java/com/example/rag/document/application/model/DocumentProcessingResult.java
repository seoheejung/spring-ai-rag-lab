package com.example.rag.document.application.model;

import java.util.List;

import org.springframework.ai.document.Document;

public record DocumentProcessingResult(
        List<Document> pageDocuments,
        List<ChunkingResult> chunkingResults
) {

    public DocumentProcessingResult {
        // 외부 변경 방지를 위한 불변 목록 생성
        pageDocuments = List.copyOf(pageDocuments);
        chunkingResults = List.copyOf(chunkingResults);
    }
}