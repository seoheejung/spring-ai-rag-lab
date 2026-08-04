package com.example.rag.evaluation.application;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.rag.evaluation.application.model.EvaluationCaseResult;
import com.example.rag.evaluation.application.model.EvaluationSummary;
import com.example.rag.evaluation.application.model.RetrievalMetrics;
import com.example.rag.evaluation.application.model.RetrievedChunk;
import org.springframework.stereotype.Component;

@Component
public class RetrievalMetricCalculator {

    public RetrievalMetrics calculate(
            boolean answerable,
            List<String> expectedChunkIds,
            List<RetrievedChunk> retrievedChunks
    ) {
        if (!answerable) {
            // 답이 없는 질문의 검색 지표 제외
            return RetrievalMetrics.notApplicable();
        }

        Set<String> expectedChunkIdSet = new HashSet<>(expectedChunkIds);

        long retrievedRelevantCount = retrievedChunks.stream()
                                        .map(RetrievedChunk::chunkId)
                                        .filter(expectedChunkIdSet::contains)
                                        .distinct()
                                        .count();

        // Hit@K 계산
        int hitAtK = retrievedRelevantCount > 0 ? 1 : 0;

        // Recall@K 계산
        double recallAtK = (double) retrievedRelevantCount / expectedChunkIdSet.size();

        // Precision@K 계산
        double precisionAtK = retrievedChunks.isEmpty()
                                ? 0.0
                                : (double) retrievedRelevantCount
                                / retrievedChunks.size();

        // 첫 번째 정답 청크 순위 조회
        int firstRelevantRank = retrievedChunks.stream()
                        .filter(chunk -> expectedChunkIdSet.contains(chunk.chunkId()))
                        .mapToInt(RetrievedChunk::rank)
                        .findFirst()
                        .orElse(0);

        // Reciprocal Rank 계산
        double reciprocalRank = firstRelevantRank == 0 ? 0.0 : 1.0 / firstRelevantRank;

        return new RetrievalMetrics(
                hitAtK,
                recallAtK,
                precisionAtK,
                reciprocalRank
        );
    }

    public EvaluationSummary summarize(
            int topK,
            double similarityThreshold,
            List<EvaluationCaseResult> results
    ) {
        List<EvaluationCaseResult> answerableResults = results.stream()
                        .filter(EvaluationCaseResult::answerable).toList();

        if (answerableResults.isEmpty()) {
            throw new IllegalStateException("답변 가능한 평가 질문이 없습니다.");
        }

        // 전체 Hit@K 계산
        double hitAtK =
                answerableResults.stream()
                        .map(EvaluationCaseResult::retrievalMetrics)
                        .mapToInt(metrics -> metrics.hitAtK())
                        .average()
                        .orElse(0.0);

        // 전체 Recall@K 계산
        double recallAtK =
                answerableResults.stream()
                        .map(EvaluationCaseResult::retrievalMetrics)
                        .mapToDouble(metrics -> metrics.recallAtK())
                        .average()
                        .orElse(0.0);

        // 전체 Precision@K 계산
        double precisionAtK =
                answerableResults.stream()
                        .map(EvaluationCaseResult::retrievalMetrics)
                        .mapToDouble(metrics -> metrics.precisionAtK())
                        .average()
                        .orElse(0.0);

        // MRR 계산
        double mrr =
                answerableResults.stream()
                        .map(EvaluationCaseResult::retrievalMetrics)
                        .mapToDouble(metrics -> metrics.reciprocalRank())
                        .average()
                        .orElse(0.0);

        // 질문 유형별 개수 집계
        Map<String, Long> questionTypeCounts = results.stream().collect(
                                Collectors.groupingBy(
                                        EvaluationCaseResult::type,
                                        Collectors.counting()
                                )
                        );

        return new EvaluationSummary(
                topK,
                similarityThreshold,
                results.size(),
                answerableResults.size(),
                results.size() - answerableResults.size(),
                questionTypeCounts,
                hitAtK,
                recallAtK,
                precisionAtK,
                mrr,
                results
        );
    }
}