package com.example.rag.evaluation.application.model;

import java.util.List;
import java.util.Map;

public record EvaluationSummary(
        int topK,
        double similarityThreshold,
        int questionCount,
        int answerableQuestionCount,
        int unanswerableQuestionCount,
        Map<String, Long> questionTypeCounts,
        double hitAtK,
        double recallAtK,
        double precisionAtK,
        double mrr,
        List<EvaluationCaseResult> results
) {

    public EvaluationSummary {
        // 외부 변경 방지를 위한 불변 결과 생성
        questionTypeCounts = Map.copyOf(questionTypeCounts);
        results = List.copyOf(results);
    }
}