package com.example.rag.graph.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;

import com.example.rag.graph.application.model.GraphPathResult;
import com.example.rag.graph.application.model.GraphQuestion;
import com.example.rag.graph.config.GraphRagProperties;
import com.example.rag.graph.infrastructure.json.GraphQuestionLoader;
import com.example.rag.graph.infrastructure.neo4j.Neo4jGraphRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GraphSearchServiceTest {

    private static final Path QUESTIONS_PATH = Path.of("evaluation/questions.json");

    private static final Path GRAPH_QUESTIONS_PATH = Path.of("evaluation/graph-questions.json");

    private static final Path RESULTS_PATH = Path.of("evaluation/results/" + "phase6-graph-rag-results.json");

    @Mock
    private GraphQuestionLoader graphQuestionLoader;

    @Mock
    private Neo4jGraphRepository neo4jGraphRepository;

    private GraphSearchService graphSearchService;

    @BeforeEach
    void setUp() {
        GraphRagProperties properties =
                new GraphRagProperties(
                        false,
                        QUESTIONS_PATH,
                        GRAPH_QUESTIONS_PATH,
                        RESULTS_PATH,
                        5,
                        0.7
                );

        graphSearchService =
                new GraphSearchService(
                        properties,
                        graphQuestionLoader,
                        neo4jGraphRepository
                );
    }

    @Test
    void 질문별_Graph_설정을_조회하고_경로를_반환한다() {
        GraphQuestion graphQuestion =
                new GraphQuestion(
                        "q-001",
                        "DOCUMENTS_BY_TOPICS",
                        List.of("지식 노드", "실행 노드")
                );

        GraphPathResult expected =
                new GraphPathResult(
                        List.of("지식 노드", "graph-engineering-v2026.08.02.pdf", "실행 노드"),
                        List.of("HAS_TOPIC", "HAS_TOPIC"),
                        List.of("graph-engineering-v2026.08.02.pdf")
                );

        when(graphQuestionLoader.load(GRAPH_QUESTIONS_PATH, QUESTIONS_PATH)).thenReturn(List.of(graphQuestion));
        when(neo4jGraphRepository.search(graphQuestion)).thenReturn(expected);

        GraphPathResult result = graphSearchService.search("q-001");

        assertThat(result).isSameAs(expected);
        assertThat(result.relationships()).containsExactly("HAS_TOPIC", "HAS_TOPIC");
        assertThat(result.documentIds()).containsExactly("graph-engineering-v2026.08.02.pdf");

        verify(graphQuestionLoader).load(GRAPH_QUESTIONS_PATH, QUESTIONS_PATH);

        ArgumentCaptor<GraphQuestion> questionCaptor = ArgumentCaptor.forClass(GraphQuestion.class);

        verify(neo4jGraphRepository).search(questionCaptor.capture());

        GraphQuestion captured = questionCaptor.getValue();

        assertThat(captured.questionId()).isEqualTo("q-001");
        assertThat(captured.queryType()).isEqualTo("DOCUMENTS_BY_TOPICS");
        assertThat(captured.entityNames()).containsExactly("지식 노드", "실행 노드");
    }

    @Test
    void 빈_Graph_결과를_그대로_반환한다() {
        GraphQuestion graphQuestion =
                new GraphQuestion(
                        "q-001",
                        "DOCUMENTS_BY_TOPICS",
                        List.of(
                                "지식 노드",
                                "실행 노드"
                        )
                );

        GraphPathResult emptyResult = new GraphPathResult(List.of(), List.of(), List.of());

        when(graphQuestionLoader.load(GRAPH_QUESTIONS_PATH, QUESTIONS_PATH)).thenReturn(List.of(graphQuestion));
        when(neo4jGraphRepository.search(graphQuestion)).thenReturn(emptyResult);

        GraphPathResult result = graphSearchService.search("q-001");

        assertThat(result.entities()).isEmpty();
        assertThat(result.relationships()).isEmpty();
        assertThat(result.documentIds()).isEmpty();

        verify(neo4jGraphRepository).search(graphQuestion);
    }
}