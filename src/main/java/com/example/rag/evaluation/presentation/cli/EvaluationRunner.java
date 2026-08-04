package com.example.rag.evaluation.presentation.cli;

import java.util.List;

import com.example.rag.evaluation.application.EvaluationService;
import com.example.rag.evaluation.application.model.EvaluationQuestion;
import com.example.rag.evaluation.application.model.EvaluationSummary;
import com.example.rag.evaluation.config.EvaluationProperties;
import com.example.rag.evaluation.infrastructure.json.EvaluationQuestionLoader;
import com.example.rag.evaluation.infrastructure.json.EvaluationResultWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "rag.evaluation",
        name = "enabled",
        havingValue = "true"
)
public class EvaluationRunner implements ApplicationRunner {

    private final EvaluationProperties properties;
    private final EvaluationQuestionLoader questionLoader;
    private final EvaluationService evaluationService;
    private final EvaluationResultWriter resultWriter;

    @Override
    public void run(ApplicationArguments args) {
        // 평가 질문 읽기
        List<EvaluationQuestion> questions = questionLoader.load(properties.questionsPath());

        // 검색·생성 평가 실행
        EvaluationSummary summary =
                evaluationService.evaluate(
                        questions,
                        properties.topK(),
                        properties.similarityThreshold()
                );

        // 평가 결과 저장
        resultWriter.write(properties.resultsPath(), summary);

        // 평가 요약 출력
        printSummary(summary);
    }

    private void printSummary(
            EvaluationSummary summary
    ) {
        System.out.println();
        System.out.println("===== Phase 4 평가 결과 =====");
        System.out.println("질문 수: " + summary.questionCount());
        System.out.println("답변 가능 질문 수: " + summary.answerableQuestionCount());
        System.out.println("답이 없는 질문 수: " + summary.unanswerableQuestionCount());
        System.out.println("질문 유형별 개수: " + summary.questionTypeCounts());
        System.out.println("Top-K: " + summary.topK());
        System.out.println("Similarity Threshold: " + summary.similarityThreshold());
        System.out.printf("Hit@%d: %.4f%n", summary.topK(), summary.hitAtK());
        System.out.printf("Recall@%d: %.4f%n", summary.topK(), summary.recallAtK());
        System.out.printf("Precision@%d: %.4f%n", summary.topK(), summary.precisionAtK());
        System.out.printf("MRR: %.4f%n", summary.mrr());
        System.out.println("결과 파일: " + properties.resultsPath());
    }
}