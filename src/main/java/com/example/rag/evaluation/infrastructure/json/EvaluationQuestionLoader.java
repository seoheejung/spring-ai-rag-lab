package com.example.rag.evaluation.infrastructure.json;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.example.rag.evaluation.application.model.EvaluationQuestion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class EvaluationQuestionLoader {

    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "exact-term",
            "semantic-paraphrase",
            "version-identifier",
            "multi-document",
            "unanswerable"
    );

    private final JsonMapper jsonMapper;

    public List<EvaluationQuestion> load(Path path) {
        // 평가 질문 파일 상태 검증
        validatePath(path);

        try {
            // 평가 질문 JSON 읽기
            EvaluationQuestion[] loadedQuestions = jsonMapper.readValue(path.toFile(), EvaluationQuestion[].class);

            List<EvaluationQuestion> questions = List.copyOf(Arrays.asList(loadedQuestions));

            // 평가 질문 데이터 검증
            validateQuestions(questions);

            return questions;
        }
        catch (JacksonException exception) {
            throw new IllegalStateException("평가 질문 파일 읽기에 실패했습니다: " + path, exception);
        }
    }

    private void validatePath(Path path) {
        // 평가 질문 파일 존재 여부 검증
        if (!Files.exists(path)) {
            throw new IllegalStateException("평가 질문 파일을 찾을 수 없습니다: " + path);
        }

        // 평가 질문 파일 읽기 가능 여부 검증
        if (!Files.isReadable(path)) {
            throw new IllegalStateException("평가 질문 파일을 읽을 수 없습니다: " + path);
        }
    }

    private void validateQuestions(
            List<EvaluationQuestion> questions
    ) {
        if (questions.isEmpty()) {
            throw new IllegalStateException("평가 질문이 비어 있습니다.");
        }

        Set<String> questionIds = new HashSet<>();

        for (EvaluationQuestion question : questions) {
            // 질문 ID 검증
            if (question.id() == null || question.id().isBlank()) {
                throw new IllegalStateException("평가 질문 ID가 비어 있습니다.");
            }

            // 질문 ID 중복 검증
            if (!questionIds.add(question.id())) {
                throw new IllegalStateException("중복 평가 질문 ID: " + question.id());
            }

            // 질문 본문 검증
            if (question.question() == null || question.question().isBlank()) {
                throw new IllegalStateException("평가 질문 본문이 비어 있습니다: " + question.id());
            }

            // 질문 유형 검증
            if (!SUPPORTED_TYPES.contains(question.type())) {
                throw new IllegalStateException("지원하지 않는 질문 유형: " + question.type());
            }

            // 기대 결과 검증
            validateExpectedResults(question);
        }
    }

    private void validateExpectedResults(
            EvaluationQuestion question
    ) {
        if (question.answerable()) {
            // 답변 가능 질문의 기대 문서 검증
            if (question.expectedDocumentIds().isEmpty()) {
                throw new IllegalStateException("기대 문서가 없습니다: " + question.id());
            }

            // 답변 가능 질문의 기대 청크 검증
            if (question.expectedChunkIds().isEmpty()) {
                throw new IllegalStateException("기대 청크가 없습니다: " + question.id());
            }

            return;
        }

        // 답이 없는 질문의 기대 문서 검증
        if (!question.expectedDocumentIds().isEmpty()) {
            throw new IllegalStateException("답이 없는 질문에 기대 문서가 지정됐습니다: " + question.id());
        }

        // 답이 없는 질문의 기대 청크 검증
        if (!question.expectedChunkIds().isEmpty()) {
            throw new IllegalStateException("답이 없는 질문에 기대 청크가 지정됐습니다: " + question.id());
        }
    }
}