package com.example.rag.graph.application;

import java.util.List;

import com.example.rag.graph.application.model.GraphPathResult;
import com.example.rag.graph.application.model.GraphQuestion;
import com.example.rag.graph.config.GraphRagProperties;
import com.example.rag.graph.infrastructure.json.GraphQuestionLoader;
import com.example.rag.graph.infrastructure.neo4j.Neo4jGraphRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GraphSearchService {

    private final GraphRagProperties graphRagProperties;
    private final GraphQuestionLoader graphQuestionLoader;
    private final Neo4jGraphRepository neo4jGraphRepository;

    public GraphPathResult search(String questionId) {
        // 질문 ID 검증
        if (questionId == null || questionId.isBlank()) {
            throw new IllegalArgumentException("Graph 질문 ID가 비어 있습니다.");
        }

        // Graph 질문 메타데이터 조회
        List<GraphQuestion> questions = graphQuestionLoader.load(
                graphRagProperties.graphQuestionsPath(),
                graphRagProperties.questionsPath()
        );

        GraphQuestion question = questions.stream()
                .filter(candidate -> candidate.questionId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Graph 질문을 찾을 수 없습니다: " + questionId)
                );

        // Graph 관계 탐색
        return neo4jGraphRepository.search(question);
    }
}