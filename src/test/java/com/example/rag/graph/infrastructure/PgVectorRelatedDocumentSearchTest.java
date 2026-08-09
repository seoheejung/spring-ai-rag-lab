package com.example.rag.graph.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

@ExtendWith(MockitoExtension.class)
class PgVectorRelatedDocumentSearchTest {

    private static final String QUESTION = "지식과 실행을 한 그래프에 합친다는 것은 무엇인가?";
    private static final String DOCUMENT_ID = "graph-engineering-v2026.08.02.pdf";

    @Mock
    private VectorStore vectorStore;

    private PgVectorRelatedDocumentSearch relatedDocumentSearch;

    @BeforeEach
    void setUp() {
        relatedDocumentSearch = new PgVectorRelatedDocumentSearch(vectorStore);
    }

    @Test
    void 관련_문서_ID를_file_name_필터로_검색한다() {
        List<String> documentIds = List.of(DOCUMENT_ID);

        List<Document> expectedDocuments =
                List.of(
                        Document.builder()
                                .text("검색 청크")
                                .metadata(Map.of("file_name", DOCUMENT_ID, "chunk_index", 461))
                                .score(0.8)
                                .build()
                );

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(expectedDocuments);

        List<Document> result =
                relatedDocumentSearch.search(
                        QUESTION,
                        documentIds,
                        5,
                        0.7
                );

        ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);

        verify(vectorStore).similaritySearch(requestCaptor.capture());

        SearchRequest request = requestCaptor.getValue();

        assertThat(request.getQuery()).isEqualTo(QUESTION);
        assertThat(request.getTopK()).isEqualTo(5);
        assertThat(request.getSimilarityThreshold()).isEqualTo(0.7);
        assertThat(request.hasFilterExpression()).isTrue();

        Filter.Expression expectedFilter = new FilterExpressionBuilder()
                .in("file_name", documentIds.toArray()).build();
        assertThat(request.getFilterExpression()).isEqualTo(expectedFilter);
        assertThat(result).isSameAs(expectedDocuments);
    }

    @Test
    void 관련_문서_ID가_없으면_VectorStore를_검색하지_않는다() {
        List<Document> result = relatedDocumentSearch.search(QUESTION, List.of(), 5, 0.7);

        assertThat(result).isEmpty();

        verify(vectorStore, never()).similaritySearch(any(SearchRequest.class));
    }
}