package com.example.rag.evaluation.application;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

import com.example.rag.evaluation.application.model.EvaluationQuestion;
import com.example.rag.evaluation.application.model.RetrievalMetrics;
import com.example.rag.evaluation.application.model.RetrievedChunk;
import com.example.rag.retrieval.application.HybridSearchService;
import com.example.rag.retrieval.application.ReciprocalRankFusion;
import com.example.rag.retrieval.application.VectorSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchComparisonService {

    private final VectorSearchService vectorSearchService;
    private final HybridSearchService hybridSearchService;
    private final RetrievalMetricCalculator metricCalculator;

    public SearchComparisonResult compare(
            List<EvaluationQuestion> questions,
            int keywordTopK,
            int vectorTopK,
            double similarityThreshold,
            int rrfK,
            int finalTopK
    ) {
        // 벡터 검색 평가
        List<SearchCaseResult> vectorResults =
                questions.stream()
                        .map(question -> evaluateVectorSearch(
                                question,
                                vectorTopK,
                                similarityThreshold
                        ))
                        .toList();

        // 하이브리드 검색 평가
        List<SearchCaseResult> hybridResults =
                questions.stream()
                        .map(question -> evaluateHybridSearch(
                                question,
                                keywordTopK,
                                vectorTopK,
                                similarityThreshold,
                                rrfK,
                                finalTopK
                        ))
                        .toList();

        // 검색 방식별 요약 생성
        return new SearchComparisonResult(
                summarize(vectorResults),
                summarize(hybridResults),
                vectorResults,
                hybridResults
        );
    }

    private SearchCaseResult evaluateVectorSearch(
            EvaluationQuestion question,
            int topK,
            double similarityThreshold
    ) {
        // 벡터 검색 시간 측정 시작
        long startedAt = System.nanoTime();

        List<Document> documents =
                vectorSearchService.search(
                        question.question(),
                        topK,
                        similarityThreshold
                );

        // 벡터 검색 시간 계산
        double elapsedMillis = toElapsedMillis(startedAt);

        // 벡터 검색 결과 평가 모델 변환
        List<RetrievedChunk> retrievedChunks =
                toVectorRetrievedChunks(
                        documents,
                        question.expectedChunkIds()
                );

        // 질문별 벡터 검색 지표 계산
        RetrievalMetrics metrics =
                metricCalculator.calculate(
                        question.answerable(),
                        question.expectedChunkIds(),
                        retrievedChunks
                );

        return new SearchCaseResult(
                question.id(),
                question.answerable(),
                retrievedChunks.size(),
                retrievedChunks,
                metrics,
                elapsedMillis
        );
    }

    private SearchCaseResult evaluateHybridSearch(
            EvaluationQuestion question,
            int keywordTopK,
            int vectorTopK,
            double similarityThreshold,
            int rrfK,
            int finalTopK
    ) {
        // 하이브리드 검색 시간 측정 시작
        long startedAt = System.nanoTime();

        List<ReciprocalRankFusion.FusedResult> fusedResults =
                hybridSearchService.search(
                        question.question(),
                        keywordTopK,
                        vectorTopK,
                        similarityThreshold,
                        rrfK,
                        finalTopK
                );

        // 하이브리드 검색 시간 계산
        double elapsedMillis = toElapsedMillis(startedAt);

        // 하이브리드 검색 결과 평가 모델 변환
        List<RetrievedChunk> retrievedChunks =
                toHybridRetrievedChunks(
                        fusedResults,
                        question.expectedChunkIds()
                );

        // 질문별 하이브리드 검색 지표 계산
        RetrievalMetrics metrics =
                metricCalculator.calculate(
                        question.answerable(),
                        question.expectedChunkIds(),
                        retrievedChunks
                );

        return new SearchCaseResult(
                question.id(),
                question.answerable(),
                retrievedChunks.size(),
                retrievedChunks,
                metrics,
                elapsedMillis
        );
    }

    private List<RetrievedChunk> toVectorRetrievedChunks(
            List<Document> documents,
            List<String> expectedChunkIds
    ) {
        // 벡터 검색 결과 변환
        return IntStream.range(0, documents.size())
                .mapToObj(index -> {
                    Document document = documents.get(index);
                    Map<String, Object> metadata = document.getMetadata();

                    String documentId = Objects.toString(metadata.get("file_name"), "");
                    String chunkIndex = Objects.toString(metadata.get("chunk_index"), "");
                    String chunkId = documentId + "#" + chunkIndex;

                    return new RetrievedChunk(
                            index + 1,
                            documentId,
                            chunkId,
                            document.getScore(),
                            expectedChunkIds.contains(chunkId),
                            metadata,
                            Objects.requireNonNullElse(document.getText(), "")
                    );
                })
                .toList();
    }

    private List<RetrievedChunk> toHybridRetrievedChunks(
            List<ReciprocalRankFusion.FusedResult> results,
            List<String> expectedChunkIds
    ) {
        // 하이브리드 검색 결과 변환
        return IntStream.range(0, results.size())
                .mapToObj(index -> {
                    ReciprocalRankFusion.FusedResult result = results.get(index);

                    String documentId = extractDocumentId(result.chunkId());

                    Map<String, Object> metadata = createHybridMetadata(result);

                    return new RetrievedChunk(
                            index + 1,
                            documentId,
                            result.chunkId(),
                            result.rrfScore(),
                            expectedChunkIds.contains(result.chunkId()),
                            metadata,
                            ""
                    );
                })
                .toList();
    }

    private Map<String, Object> createHybridMetadata(
            ReciprocalRankFusion.FusedResult result
    ) {
        // 하이브리드 검색 메타데이터 생성
        Map<String, Object> metadata = new HashMap<>();

        metadata.put("chunk_id", result.chunkId());
        metadata.put("rrf_score", result.rrfScore());

        if (result.keywordRank() != null) {
            metadata.put("keyword_rank", result.keywordRank());
        }

        if (result.vectorRank() != null) {
            metadata.put("vector_rank", result.vectorRank());
        }

        return metadata;
    }

    private String extractDocumentId(
            String chunkId
    ) {
        // 문서 ID 구분 위치 조회
        int separatorIndex = chunkId.lastIndexOf('#');

        if (separatorIndex <= 0) {
            throw new IllegalStateException("올바르지 않은 청크 ID입니다: " + chunkId);
        }

        // 문서 ID 추출
        return chunkId.substring(0, separatorIndex);
    }

    private double toElapsedMillis(
            long startedAt
    ) {
        // 검색 경과 시간 계산
        long elapsedNanos = System.nanoTime() - startedAt;

        return elapsedNanos / 1_000_000.0;
    }

    private SearchMetricSummary summarize(
            List<SearchCaseResult> results
    ) {
        // 답변 가능 질문 필터링
        List<SearchCaseResult> answerableResults =
                results.stream()
                        .filter(SearchCaseResult::answerable)
                        .toList();

        if (answerableResults.isEmpty()) {
            throw new IllegalStateException("답변 가능한 평가 질문이 없습니다.");
        }

        // 평균 Hit@5 계산
        double hitAtK = answerableResults.stream()
                        .map(SearchCaseResult::metrics)
                        .mapToInt(RetrievalMetrics::hitAtK)
                        .average()
                        .orElse(0.0);

        // 평균 Recall@5 계산
        double recallAtK = answerableResults.stream()
                        .map(SearchCaseResult::metrics)
                        .mapToDouble(RetrievalMetrics::recallAtK)
                        .average()
                        .orElse(0.0);

        // 평균 Precision@5 계산
        double precisionAtK = answerableResults.stream()
                        .map(SearchCaseResult::metrics)
                        .mapToDouble(RetrievalMetrics::precisionAtK)
                        .average()
                        .orElse(0.0);

        // MRR 계산
        double mrr = answerableResults.stream()
                        .map(SearchCaseResult::metrics)
                        .mapToDouble(RetrievalMetrics::reciprocalRank)
                        .average()
                        .orElse(0.0);

        // 평균 검색 시간 계산
        double averageSearchMillis = results.stream()
                        .mapToDouble(SearchCaseResult::elapsedMillis)
                        .average()
                        .orElse(0.0);

        return new SearchMetricSummary(
                hitAtK,
                recallAtK,
                precisionAtK,
                mrr,
                averageSearchMillis
        );
    }

    public record SearchComparisonResult(
            SearchMetricSummary vectorSummary,
            SearchMetricSummary hybridSummary,
            List<SearchCaseResult> vectorResults,
            List<SearchCaseResult> hybridResults
    ) {

        public SearchComparisonResult {
            // 외부 변경 방지를 위한 불변 결과 생성
            vectorResults = List.copyOf(vectorResults);
            hybridResults = List.copyOf(hybridResults);
        }
    }

    public record SearchCaseResult(
            String questionId,
            boolean answerable,
            int resultCount,
            List<RetrievedChunk> retrievedChunks,
            RetrievalMetrics metrics,
            double elapsedMillis
    ) {

        public SearchCaseResult {
            // 외부 변경 방지를 위한 불변 검색 결과 생성
            retrievedChunks = List.copyOf(retrievedChunks);
        }
    }

    public record SearchMetricSummary(
            double hitAtK,
            double recallAtK,
            double precisionAtK,
            double mrr,
            double averageSearchMillis
    ) {
    }
}