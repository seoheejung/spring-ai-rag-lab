package com.example.rag.graph.application;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

@Component
public class GraphDocumentMerger {

    public List<Document> merge(
            List<Document> vectorDocuments,
            List<Document> graphDocuments,
            int limit
    ) {
        // 청크 ID 기준 순서 보존 병합
        Map<String, Document> merged = new LinkedHashMap<>();

        // 기존 벡터 검색 순서 유지
        vectorDocuments.forEach(document -> merged.putIfAbsent(createChunkId(document), document));

        // Graph 검색 신규 청크 추가
        graphDocuments.forEach(document -> merged.putIfAbsent(createChunkId(document), document));

        // 최종 Context 청크 수 제한
        return merged.values()
                .stream()
                .limit(limit)
                .toList();
    }

    private String createChunkId(
            Document document
    ) {
        // 문서명과 청크 번호 조회
        Object fileName = document.getMetadata().get("file_name");
        Object chunkIndex = document.getMetadata().get("chunk_index");

        // 청크 식별자 생성
        return fileName + "#" + chunkIndex;
    }
}