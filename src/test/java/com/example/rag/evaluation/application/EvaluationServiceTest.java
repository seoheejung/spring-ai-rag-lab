package com.example.rag.evaluation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import com.example.rag.evaluation.application.model.EvaluationQuestion;
import com.example.rag.evaluation.application.model.EvaluationSummary;
import com.example.rag.generation.application.RagService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

class EvaluationServiceTest {

    @Test
    void 질문별_검색과_수동_RAG_답변을_평가한다() {
        // 의존성 Mock 생성
        RagService ragService = mock(RagService.class);

        RetrievalMetricCalculator calculator = new RetrievalMetricCalculator();

        EvaluationService evaluationService = new EvaluationService(ragService, calculator);

        // 평가 질문 생성
        EvaluationQuestion question =
                new EvaluationQuestion(
                        "q-001",
                        "평가 질문",
                        "exact-term",
                        List.of("document.pdf"),
                        List.of("document.pdf#10"),
                        true
                );

        // 검색 결과 Document 생성
        Document document = mock(Document.class);

        when(document.getMetadata()).thenReturn(Map.of("file_name", "document.pdf", "chunk_index", 10));
        when(document.getText()).thenReturn("정답 근거");
        when(document.getScore()).thenReturn(0.9);
        when(ragService.retrieve("평가 질문", 5, 0.7)).thenReturn(List.of(document));
        when(ragService.answerManually("평가 질문", List.of(document))).thenReturn("생성 답변");

        // 평가 실행
        EvaluationSummary summary = evaluationService.evaluate(List.of(question), 5, 0.7);

        // 검색 호출 검증
        verify(ragService).retrieve("평가 질문", 5, 0.7);

        // 수동 RAG 호출 검증
        verify(ragService).answerManually("평가 질문", List.of(document));

        // 전체 지표 검증
        assertThat(summary.hitAtK()).isEqualTo(1.0);
        assertThat(summary.recallAtK()).isEqualTo(1.0);
        assertThat(summary.precisionAtK()).isEqualTo(1.0);
        assertThat(summary.mrr()).isEqualTo(1.0);

        // 질문별 결과 검증
        var result = summary.results().getFirst();

        assertThat(result.retrievedChunks()).hasSize(1);
        assertThat(result.retrievedChunks().getFirst().rank()).isEqualTo(1);
        assertThat(result.retrievedChunks().getFirst().chunkId()).isEqualTo("document.pdf#10");
        assertThat(result.retrievedChunks().getFirst().relevant()).isTrue();
        assertThat(result.answer()).isEqualTo("생성 답변");
        assertThat(result.failureType()).isEqualTo("UNREVIEWED");
        assertThat(result.groundedness()).isNull();
    }
}