package com.example.rag.graph.infrastructure.json;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.rag.evaluation.application.model.EvaluationQuestion;
import com.example.rag.evaluation.infrastructure.json.EvaluationQuestionLoader;
import com.example.rag.graph.application.model.GraphQuestion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class GraphQuestionLoader {

    private static final Set<String> REQUIRED_QUESTION_IDS = Set.of("q-001", "q-002");
    private static final Set<String> SUPPORTED_QUERY_TYPES = Set.of("DOCUMENTS_BY_TOPICS");

    private static final int REQUIRED_ENTITY_COUNT = 2;

    private final JsonMapper jsonMapper;
    private final EvaluationQuestionLoader evaluationQuestionLoader;

    public List<GraphQuestion> load(
            Path graphQuestionsPath,
            Path questionsPath
    ) {
        // Graph 질문 파일 상태 검증
        validatePath(graphQuestionsPath);

        // Graph 질문 JSON 읽기
        GraphQuestion[] questions =
                jsonMapper.readValue(
                        graphQuestionsPath.toFile(),
                        GraphQuestion[].class
                );

        if (questions == null || questions.length == 0) {
            throw new IllegalStateException("Graph 질문이 비어 있습니다.");
        }

        List<GraphQuestion> result = List.copyOf(Arrays.asList(questions));

        // 기존 평가 질문 조회
        List<EvaluationQuestion> evaluationQuestions = evaluationQuestionLoader.load(questionsPath);

        // Graph 질문 데이터 검증
        validateQuestions(
                result,
                evaluationQuestions
        );

        return result;
    }

    private void validatePath(Path path) {
        // Graph 질문 파일 존재 여부 검증
        if (!Files.exists(path)) {
            throw new IllegalStateException("Graph 질문 파일을 찾을 수 없습니다: " + path);
        }

        // Graph 질문 파일 읽기 가능 여부 검증
        if (!Files.isReadable(path)) {
            throw new IllegalStateException("Graph 질문 파일을 읽을 수 없습니다: " + path);
        }
    }

    private void validateQuestions(
            List<GraphQuestion> graphQuestions,
            List<EvaluationQuestion> evaluationQuestions
    ) {
        Set<String> evaluationQuestionIds = evaluationQuestions.stream()
                .map(EvaluationQuestion::id)
                .collect(Collectors.toSet());

        Set<String> graphQuestionIds = new HashSet<>();

        for (GraphQuestion question : graphQuestions) {
            validateQuestionId(
                    question,
                    graphQuestionIds,
                    evaluationQuestionIds
            );

            validateQueryType(question);
            validateEntityNames(question);
        }

        // Phase 6 대상 질문 구성 검증
        if (!graphQuestionIds.equals(REQUIRED_QUESTION_IDS)) {
            throw new IllegalStateException("Graph 질문 ID는 q-001, q-002만 포함해야 합니다: " + graphQuestionIds);
        }
    }

    private void validateQuestionId(
            GraphQuestion question,
            Set<String> graphQuestionIds,
            Set<String> evaluationQuestionIds
    ) {
        // 질문 ID 누락 검증
        if (question.questionId() == null || question.questionId().isBlank()) {
            throw new IllegalStateException("Graph 질문 ID가 비어 있습니다.");
        }

        // 질문 ID 중복 검증
        if (!graphQuestionIds.add(question.questionId())) {
            throw new IllegalStateException("중복 Graph 질문 ID: " + question.questionId());
        }

        // 평가 질문 ID 존재 여부 검증
        if (!evaluationQuestionIds.contains(question.questionId())) {
            throw new IllegalStateException("평가 질문에 존재하지 않는 Graph 질문 ID: " + question.questionId());
        }
    }

    private void validateQueryType(GraphQuestion question) {
        // 탐색 유형 누락 검증
        if (question.queryType() == null || question.queryType().isBlank()) {
            throw new IllegalStateException("Graph 탐색 유형이 비어 있습니다: " + question.questionId());
        }

        // 지원 탐색 유형 검증
        if (!SUPPORTED_QUERY_TYPES.contains(question.queryType())) {
            throw new IllegalStateException("지원하지 않는 Graph 탐색 유형: " + question.queryType());
        }
    }

    private void validateEntityNames(GraphQuestion question) {
        // 엔티티 목록 누락 검증
        if (question.entityNames() == null || question.entityNames().isEmpty()) {
            throw new IllegalStateException("Graph 탐색 엔티티가 없습니다: " + question.questionId());
        }

        // 엔티티 이름 누락 검증
        if (question.entityNames().stream().anyMatch(name -> name == null || name.isBlank())) {
            throw new IllegalStateException("Graph 탐색 엔티티 이름이 비어 있습니다: " + question.questionId());
        }

        // 탐색 유형별 엔티티 수 검증
        if (question.entityNames().size() != REQUIRED_ENTITY_COUNT) {
            throw new IllegalStateException("DOCUMENTS_BY_TOPICS는 엔티티 2개가 필요합니다: " + question.questionId());
        }
    }
}