package com.example.rag.retrieval.application;

import java.util.List;

import com.example.rag.document.application.DocumentChunkingService;
import com.example.rag.document.application.model.ChunkingScenario;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VectorSearchService {

    // Phase 2 저장 기준 청킹 조건
    private static final ChunkingScenario STORAGE_SCENARIO =
            new ChunkingScenario(
                    "B",
                    800,
                    true,
                    "Phase 2 벡터 저장 기준"
            );

    private final DocumentChunkingService documentChunkingService;
    private final VectorStore vectorStore;

    public int store() {
        // 저장 대상 청크 생성
        List<Document> chunks = documentChunkingService.createChunks(STORAGE_SCENARIO);

        // 청크 임베딩과 Vector Store 저장
        vectorStore.add(chunks);

        // 저장 요청 청크 수 반환
        return chunks.size();
    }

    public List<Document> search(
            String question,
            int topK,
            double similarityThreshold
    ) {
        // 유사도 검색 조건 생성
        SearchRequest request =
                SearchRequest.builder()
                        .query(question)
                        .topK(topK)
                        .similarityThreshold(similarityThreshold)
                        .build();

        // 질문 기반 유사 문서 검색
        return vectorStore.similaritySearch(request);
    }
}