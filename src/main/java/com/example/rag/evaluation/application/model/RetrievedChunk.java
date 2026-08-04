package com.example.rag.evaluation.application.model;

import java.util.Map;

public record RetrievedChunk(
        int rank,
        String documentId,
        String chunkId,
        Double score,
        boolean relevant,
        Map<String, Object> metadata,
        String content
) {

    public RetrievedChunk {
        // 외부 변경 방지를 위한 불변 메타데이터 생성
        metadata = Map.copyOf(metadata);
    }
}