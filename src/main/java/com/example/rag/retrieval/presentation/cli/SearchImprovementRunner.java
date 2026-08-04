package com.example.rag.retrieval.presentation.cli;

import java.util.List;

import com.example.rag.evaluation.application.model.EvaluationQuestion;
import com.example.rag.evaluation.infrastructure.json.EvaluationQuestionLoader;
import com.example.rag.retrieval.application.HybridSearchService;
import com.example.rag.retrieval.application.ReciprocalRankFusion;
import com.example.rag.retrieval.config.SearchImprovementProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import com.example.rag.evaluation.application.SearchComparisonService;
import com.example.rag.evaluation.infrastructure.json.SearchComparisonResultWriter;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "rag.improvement",
        name = "enabled",
        havingValue = "true"
)
public class SearchImprovementRunner implements ApplicationRunner {

    private final SearchImprovementProperties properties;
    private final EvaluationQuestionLoader questionLoader;
    private final HybridSearchService hybridSearchService;
    private final SearchComparisonService searchComparisonService;
    private final SearchComparisonResultWriter resultWriter;

    @Override
    public void run(ApplicationArguments args) {
        // 평가 질문 읽기
        List<EvaluationQuestion> questions =
                questionLoader.load(
                        properties.questionsPath()
                );

        // 하이브리드 검색 설정 출력
        printSettings(questions.size());

        // 질문별 하이브리드 검색과 후보 검색 실행
        int totalCandidateCount = 0;

        for (EvaluationQuestion question : questions) {
            runSearch(question);
            totalCandidateCount +=
                    runCandidateSearch(question);
        }

        // 검색 방식별 품질과 시간 평가
        SearchComparisonService.SearchComparisonResult comparison =
                searchComparisonService.compare(
                        questions,
                        properties.keywordTopK(),
                        properties.vectorTopK(),
                        properties.similarityThreshold(),
                        properties.rrfK(),
                        properties.finalTopK()
                );

        // 검색 방식 비교 결과 출력
        printComparison(comparison);

        // 리랭킹 수행 상태 생성
        SearchComparisonResultWriter.RerankingExecution reranking =
                new SearchComparisonResultWriter.RerankingExecution(
                        "NOT_EXECUTED",
                        totalCandidateCount == 0
                                ? "RRF 후보가 생성되지 않아 리랭킹을 수행하지 않음"
                                : "리랭킹 기능을 수행하지 않음",
                        totalCandidateCount
                );

        // Phase 5 검색 결과 저장
        resultWriter.write(
                properties.baselineResultsPath(),
                properties.resultsPath(),
                new SearchComparisonResultWriter.SearchExecutionSettings(
                        questions.size(),
                        properties.keywordTopK(),
                        properties.vectorTopK(),
                        properties.similarityThreshold(),
                        properties.rrfK(),
                        properties.candidateCount(),
                        properties.finalTopK()
                ),
                comparison,
                reranking
        );

        // 검색 결과 파일 경로 출력
        System.out.println();
        System.out.println(
                "검색 결과 파일: "
                        + properties.resultsPath()
        );
    }

    private void runSearch(
            EvaluationQuestion question
    ) {
        List<ReciprocalRankFusion.FusedResult> results =
                hybridSearchService.search(
                        question.question(),
                        properties.keywordTopK(),
                        properties.vectorTopK(),
                        properties.similarityThreshold(),
                        properties.rrfK(),
                        properties.finalTopK()
                );

        printResults(question, results);
    }

    private int runCandidateSearch(
            EvaluationQuestion question
    ) {
        // 리랭킹 후보 검색
        List<ReciprocalRankFusion.FusedResult> candidates =
                hybridSearchService.search(
                        question.question(),
                        properties.candidateCount(),
                        properties.candidateCount(),
                        properties.similarityThreshold(),
                        properties.rrfK(),
                        properties.candidateCount()
                );

        // 리랭킹 후보 출력
        printCandidateResults(question, candidates);

        // 리랭킹 후보 수 반환
        return candidates.size();
    }

    private void printSettings(
            int questionCount
    ) {
        System.out.println();
        System.out.println("===== Phase 5 하이브리드 검색 =====");
        System.out.println("질문 수: " + questionCount);
        System.out.println("Keyword Top-K: " + properties.keywordTopK());
        System.out.println("Vector Top-K: " + properties.vectorTopK());
        System.out.println("Similarity Threshold: " + properties.similarityThreshold());
        System.out.println("RRF k: " + properties.rrfK());
        System.out.println("Final Top-K: " + properties.finalTopK());
    }

    private void printResults(
            EvaluationQuestion question,
            List<ReciprocalRankFusion.FusedResult> results
    ) {
        System.out.println();
        System.out.println("===== 질문 " + question.id() + " =====");
        System.out.println("질문: " + question.question());
        System.out.println("검색 결과 수: " + results.size());

        if (results.isEmpty()) {
            System.out.println("검색 결과 없음");
            return;
        }

        for (int index = 0; index < results.size(); index++) {
            ReciprocalRankFusion.FusedResult result = results.get(index);

            boolean expected = question.expectedChunkIds().contains(result.chunkId());

            String expectedResult = question.answerable() ? expected ? "포함" : "미포함" : "해당 없음";

            System.out.printf(
                    "%d위 | 청크 ID: %s"
                            + " | 키워드 순위: %s"
                            + " | 벡터 순위: %s"
                            + " | RRF 점수: %.8f"
                            + " | 기대 청크: %s%n",
                    index + 1,
                    result.chunkId(),
                    formatRank(result.keywordRank()),
                    formatRank(result.vectorRank()),
                    result.rrfScore(),
                    expectedResult
            );
        }
    }

    private void printCandidateResults(
            EvaluationQuestion question,
            List<ReciprocalRankFusion.FusedResult> candidates
    ) {
        System.out.println();
        System.out.println("===== 리랭킹 후보 " + question.id() + " =====");
        System.out.println("후보 수: " + candidates.size());

        for (int index = 0; index < candidates.size(); index++) {
            ReciprocalRankFusion.FusedResult candidate =
                    candidates.get(index);

            boolean expected = question.expectedChunkIds().contains(candidate.chunkId());

            String expectedResult = question.answerable() ? expected ? "포함" : "미포함" : "해당 없음";

            System.out.printf(
                    "%d위 | 청크 ID: %s"
                            + " | 키워드 순위: %s"
                            + " | 벡터 순위: %s"
                            + " | RRF 점수: %.8f"
                            + " | 기대 청크: %s%n",
                    index + 1,
                    candidate.chunkId(),
                    formatRank(candidate.keywordRank()),
                    formatRank(candidate.vectorRank()),
                    candidate.rrfScore(),
                    expectedResult
            );
        }
    }

    private void printComparison(
            SearchComparisonService.SearchComparisonResult comparison
    ) {
        System.out.println();
        System.out.println("===== 검색 품질과 시간 비교 =====");

        printSummary("Phase 5 벡터 검색", comparison.vectorSummary());
        printSummary("하이브리드 검색", comparison.hybridSummary());

        printCaseResults("Phase 5 벡터 검색", comparison.vectorResults());
        printCaseResults("하이브리드 검색", comparison.hybridResults());

        System.out.println();
        System.out.println("하이브리드 + 리랭킹: 미수행");
    }

    private void printSummary(
            String name,
            SearchComparisonService.SearchMetricSummary summary
    ) {
        System.out.println();
        System.out.println("[" + name + "]");
        System.out.printf("Hit@5: %.4f%n", summary.hitAtK());
        System.out.printf("Recall@5: %.4f%n", summary.recallAtK());
        System.out.printf("Precision@5: %.4f%n", summary.precisionAtK());
        System.out.printf("MRR: %.4f%n", summary.mrr());
        System.out.printf("평균 검색 시간: %.4f ms%n", summary.averageSearchMillis());
    }

    private void printCaseResults(
            String name,
            List<SearchComparisonService.SearchCaseResult> results
    ) {
        System.out.println();
        System.out.println("[" + name + " 질문별 결과]");

        results.forEach(result ->
                System.out.printf("%s | 결과 수: %d" + " | 검색 시간: %.4f ms%n",
                        result.questionId(),
                        result.resultCount(),
                        result.elapsedMillis()
                )
        );
    }

    private String formatRank(
            Integer rank
    ) {
        return rank == null ? "-" : rank.toString();
    }
}