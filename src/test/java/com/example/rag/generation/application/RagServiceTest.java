package com.example.rag.generation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private ChatClient chatClient;

    @InjectMocks
    private RagService ragService;

    @Test
    void 질문과_TopK와_임계값으로_문서를_검색한다() {
        // 검색 결과 생성
        List<Document> expectedResults = List.of(new Document("검색 결과"));

        when(vectorStore.similaritySearch(
                any(SearchRequest.class)
        )).thenReturn(expectedResults);

        // 문서 검색
        List<Document> actualResults =
                ragService.retrieve(
                        "질문",
                        5,
                        0.7
                );

        // SearchRequest 캡처
        ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);

        verify(vectorStore).similaritySearch(requestCaptor.capture());

        SearchRequest request = requestCaptor.getValue();

        // 검색 조건 검증
        assertThat(request.getQuery()).isEqualTo("질문");
        assertThat(request.getTopK()).isEqualTo(5);
        assertThat(request.getSimilarityThreshold()).isEqualTo(0.7);

        // 검색 결과 검증
        assertThat(actualResults).isEqualTo(expectedResults);
    }

    @Test
    void 검색_문서의_메타데이터와_본문으로_Context를_생성한다() {
        // 검색 문서 생성
        Document firstDocument =
                new Document(
                        "첫 번째 청크 본문",
                        Map.of(
                                "file_name",
                                "document.pdf",
                                "page_number",
                                1,
                                "end_page_number",
                                453,
                                "chunk_index",
                                10
                        )
                );

        Document secondDocument =
                new Document(
                        "두 번째 청크 본문",
                        Map.of(
                                "file_name",
                                "document.pdf",
                                "page_number",
                                1,
                                "end_page_number",
                                453,
                                "chunk_index",
                                20
                        )
                );

        // Context 생성
        String context = ragService.createContext(List.of(firstDocument, secondDocument));

        // 문서 정보 포함 검증
        assertThat(context)
                .contains(
                        "문서명: document.pdf",
                        "저장된 페이지 범위: 1-453",
                        "청크 번호: 10",
                        "첫 번째 청크 본문",
                        "청크 번호: 20",
                        "두 번째 청크 본문"
                );

        // 검색 순서 유지 검증
        assertThat(context.indexOf("청크 번호: 10")).isLessThan(context.indexOf("청크 번호: 20"));

        // 검색되지 않은 문서 제외 검증
        assertThat(context).doesNotContain("검색되지 않은 청크 본문");
    }
}