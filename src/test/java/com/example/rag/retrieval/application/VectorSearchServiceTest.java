package com.example.rag.retrieval.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.example.rag.document.application.DocumentChunkingService;
import com.example.rag.document.application.model.ChunkingScenario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

@ExtendWith(MockitoExtension.class)
class VectorSearchServiceTest {

    @Mock
    private DocumentChunkingService documentChunkingService;

    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private VectorSearchService vectorSearchService;

    @Test
    void Phase1_기준_청크를_VectorStore에_저장한다() {
        // 저장 대상 청크 생성
        List<Document> chunks = List.of(
                new Document("청크 1"),
                new Document("청크 2")
        );

        when(documentChunkingService.createChunks(
                any(ChunkingScenario.class)
        )).thenReturn(chunks);

        // 문서 청크 저장
        int storedChunkCount = vectorSearchService.store();

        // 청킹 조건 캡처
        ArgumentCaptor<ChunkingScenario> scenarioCaptor =
                ArgumentCaptor.forClass(ChunkingScenario.class);

        verify(documentChunkingService).createChunks(scenarioCaptor.capture());

        ChunkingScenario scenario = scenarioCaptor.getValue();

        // Phase 2 저장 기준 검증
        assertThat(scenario.code()).isEqualTo("B");
        assertThat(scenario.chunkSize()).isEqualTo(800);
        assertThat(scenario.keepSeparator()).isTrue();

        // Vector Store 저장 호출 검증
        verify(vectorStore).add(chunks);

        // 저장 요청 청크 수 검증
        assertThat(storedChunkCount).isEqualTo(chunks.size());
    }

    @Test
    void 질문과_TopK와_임계값으로_유사도_검색한다() {
        // 검색 결과 생성
        List<Document> expectedResults = List.of(new Document("검색 결과"));

        when(vectorStore.similaritySearch(
                any(SearchRequest.class)
        )).thenReturn(expectedResults);

        // 유사도 검색
        List<Document> actualResults =
                vectorSearchService.search(
                        "임베딩의 역할은 무엇인가?",
                        5,
                        0.7
                );

        // SearchRequest 캡처
        ArgumentCaptor<SearchRequest> requestCaptor =
                ArgumentCaptor.forClass(
                        SearchRequest.class
                );

        verify(vectorStore)
                .similaritySearch(
                        requestCaptor.capture()
                );

        SearchRequest request = requestCaptor.getValue();

        // 검색 조건 검증
        assertThat(request.getQuery()).isEqualTo("임베딩의 역할은 무엇인가?");
        assertThat(request.getTopK()).isEqualTo(5);
        assertThat(request.getSimilarityThreshold()).isEqualTo(0.7);

        // 검색 결과 반환 검증
        assertThat(actualResults).isEqualTo(expectedResults);
    }
}