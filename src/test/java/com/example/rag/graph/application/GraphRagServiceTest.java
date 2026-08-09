package com.example.rag.graph.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.example.rag.generation.application.RagService;
import com.example.rag.graph.application.model.GraphPathResult;
import com.example.rag.graph.application.model.GraphQuestion;
import com.example.rag.graph.application.model.GraphRagCaseResult;
import com.example.rag.graph.application.model.GraphRetrievalResult;
import com.example.rag.graph.config.GraphRagProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;

@ExtendWith(MockitoExtension.class)
class GraphRagServiceTest {

    private static final String QUESTION = "지식과 실행을 한 그래프에 합친다는 것은 무엇인가?";
    private static final String DOCUMENT_ID = "graph-engineering-v2026.08.02.pdf";
    private static final int TOP_K = 5;
    private static final double SIMILARITY_THRESHOLD = 0.7;

    @Mock
    private RagService ragService;

    @Mock
    private GraphSearchService graphSearchService;

    @Mock
    private RelatedDocumentSearch relatedDocumentSearch;

    @Mock
    private GraphDocumentMerger graphDocumentMerger;

    private GraphRagService graphRagService;

    @BeforeEach
    void setUp() {
        GraphRagProperties properties =
                new GraphRagProperties(
                        false,
                        Path.of("evaluation/questions.json"),
                        Path.of("evaluation/graph-questions.json"),
                        Path.of("evaluation/results/" + "phase6-graph-rag-results.json"),
                        TOP_K,
                        SIMILARITY_THRESHOLD
                );

        graphRagService =
                new GraphRagService(
                        ragService,
                        graphSearchService,
                        relatedDocumentSearch,
                        graphDocumentMerger,
                        properties
                );
    }

    @Test
    void 일반_RAG와_Graph_기반_RAG를_각각_실행한다() {
        GraphQuestion graphQuestion = graphQuestion();
        GraphPathResult graphPath = graphPath();

        List<Document> vectorDocuments = List.of(document(10, "일반 벡터 검색 청크"));
        List<Document> graphDocuments = List.of(document(461, "Graph 문서 범위 검색 청크"));
        List<Document> mergedDocuments = List.of(vectorDocuments.get(0), graphDocuments.get(0));

        when(ragService.retrieve(QUESTION, TOP_K, SIMILARITY_THRESHOLD)).thenReturn(vectorDocuments);
        when(ragService.answerManually(QUESTION, vectorDocuments)).thenReturn("일반 RAG 답변");
        when(graphSearchService.search("q-001")).thenReturn(graphPath);
        when(relatedDocumentSearch.search(QUESTION, List.of(DOCUMENT_ID), TOP_K, SIMILARITY_THRESHOLD)).thenReturn(graphDocuments);
        when(graphDocumentMerger.merge(vectorDocuments, graphDocuments, TOP_K)).thenReturn(mergedDocuments);
        when(ragService.answerManually(QUESTION, mergedDocuments)).thenReturn("Graph 기반 RAG 답변");

        GraphRagCaseResult result = graphRagService.execute(QUESTION, graphQuestion);

        assertThat(result.questionId()).isEqualTo("q-001");
        assertThat(result.question()).isEqualTo(QUESTION);
        assertThat(result.generalAnswer()).isEqualTo("일반 RAG 답변");
        assertThat(result.graphAnswer()).isEqualTo("Graph 기반 RAG 답변");

        GraphRetrievalResult retrievalResult = result.retrievalResult();

        assertThat(retrievalResult.vectorDocuments()).containsExactlyElementsOf(vectorDocuments);
        assertThat(retrievalResult.graphDocuments()).containsExactlyElementsOf(graphDocuments);
        assertThat(retrievalResult.mergedDocuments()).containsExactlyElementsOf(mergedDocuments);
        assertThat(retrievalResult.relatedDocumentIds()).containsExactly(DOCUMENT_ID);
        assertThat(retrievalResult.graphRelationships()).containsExactly("HAS_TOPIC", "HAS_TOPIC");
        assertThat(retrievalResult.status()).isEqualTo(GraphRetrievalResult.Status.SUCCESS);

        verify(ragService).retrieve(QUESTION, TOP_K, SIMILARITY_THRESHOLD);
        verify(graphSearchService).search("q-001");
        verify(relatedDocumentSearch).search(QUESTION, List.of(DOCUMENT_ID), TOP_K, SIMILARITY_THRESHOLD);
        verify(graphDocumentMerger).merge(vectorDocuments, graphDocuments, TOP_K);
        verify(ragService).answerManually(QUESTION, vectorDocuments);
        verify(ragService).answerManually(QUESTION, mergedDocuments);
    }

    @Test
    void Graph_경로가_없으면_관련_문서_검색을_실행하지_않는다() {
        GraphQuestion graphQuestion = graphQuestion();
        GraphPathResult emptyGraphPath = new GraphPathResult(List.of(), List.of(), List.of());

        List<Document> vectorDocuments = List.of(document(10, "일반 벡터 검색 청크"));

        when(ragService.retrieve(QUESTION, TOP_K, SIMILARITY_THRESHOLD)).thenReturn(vectorDocuments);
        when(ragService.answerManually(QUESTION, vectorDocuments)).thenReturn("일반 벡터 답변");
        when(graphSearchService.search("q-001")).thenReturn(emptyGraphPath);
        when(graphDocumentMerger.merge(vectorDocuments, List.of(), TOP_K)).thenReturn(vectorDocuments);

        GraphRagCaseResult result = graphRagService.execute(QUESTION, graphQuestion);

        assertThat(result.retrievalResult().status()).isEqualTo(GraphRetrievalResult.Status.GRAPH_PATH_NOT_FOUND);
        assertThat(result.retrievalResult().graphDocuments()).isEmpty();
        assertThat(result.retrievalResult().mergedDocuments()).containsExactlyElementsOf(vectorDocuments);

        verify(relatedDocumentSearch, never()).search(QUESTION, List.of(), TOP_K, SIMILARITY_THRESHOLD);
        verify(ragService, times(2)).answerManually(QUESTION, vectorDocuments);
    }

    @Test
    void 관련_문서_검색_결과가_없으면_상태를_기록한다() {
        GraphQuestion graphQuestion = graphQuestion();
        GraphPathResult graphPath = graphPath();

        List<Document> vectorDocuments = List.of(document(10, "일반 벡터 검색 청크"));

        when(ragService.retrieve(QUESTION, TOP_K, SIMILARITY_THRESHOLD)).thenReturn(vectorDocuments);
        when(ragService.answerManually(QUESTION, vectorDocuments)).thenReturn("일반 벡터 답변");
        when(graphSearchService.search("q-001")).thenReturn(graphPath);
        when(relatedDocumentSearch.search(QUESTION, List.of(DOCUMENT_ID), TOP_K, SIMILARITY_THRESHOLD)).thenReturn(List.of());
        when(graphDocumentMerger.merge(vectorDocuments, List.of(), TOP_K)).thenReturn(vectorDocuments);

        GraphRagCaseResult result = graphRagService.execute(QUESTION, graphQuestion);

        assertThat(result.retrievalResult().status()).isEqualTo(GraphRetrievalResult.Status.GRAPH_DOCUMENT_SEARCH_EMPTY);
        assertThat(result.retrievalResult().relatedDocumentIds()).containsExactly(DOCUMENT_ID);
        assertThat(result.retrievalResult().graphDocuments()).isEmpty();
        assertThat(result.retrievalResult().mergedDocuments()).containsExactlyElementsOf(vectorDocuments);

        verify(relatedDocumentSearch).search(QUESTION, List.of(DOCUMENT_ID), TOP_K, SIMILARITY_THRESHOLD);
        verify(graphDocumentMerger).merge(vectorDocuments, List.of(), TOP_K);
        verify(ragService, times(2)).answerManually(QUESTION, vectorDocuments);
    }

    private GraphQuestion graphQuestion() {
        return new GraphQuestion(
                "q-001",
                "DOCUMENTS_BY_TOPICS",
                List.of("지식 노드", "실행 노드")
        );
    }

    private GraphPathResult graphPath() {
        return new GraphPathResult(
                List.of("지식 노드", DOCUMENT_ID, "실행 노드"),
                List.of("HAS_TOPIC", "HAS_TOPIC"),
                List.of(DOCUMENT_ID)
        );
    }

    private Document document(int chunkIndex, String text) {
        return Document.builder()
                .text(text)
                .metadata(Map.of("file_name", DOCUMENT_ID, "chunk_index", chunkIndex))
                .build();
    }
}