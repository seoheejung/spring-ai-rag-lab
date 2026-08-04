package com.example.rag.retrieval.application;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class ReciprocalRankFusion {

    public List<FusedResult> fuse(
            List<RankedResult> keywordResults,
            List<RankedResult> vectorResults,
            int k,
            int limit
    ) {
        // RRF 설정 검증
        if (k <= 0) {
            throw new IllegalArgumentException("RRF k는 1 이상이어야 합니다.");
        }

        Map<String, MutableFusedResult> merged = new HashMap<>();

        // 키워드 검색 순위 반영
        keywordResults.forEach(result -> {
            MutableFusedResult fusedResult =
                    merged.computeIfAbsent(
                            result.chunkId(),
                            MutableFusedResult::new
                    );

            fusedResult.keywordRank = result.rank();
            fusedResult.score += calculateScore(k, result.rank());
        });

        // 벡터 검색 순위 반영
        vectorResults.forEach(result -> {
            MutableFusedResult fusedResult =
                    merged.computeIfAbsent(
                            result.chunkId(),
                            MutableFusedResult::new
                    );

            fusedResult.vectorRank = result.rank();
            fusedResult.score += calculateScore(k, result.rank());
        });

        // RRF 통합 순위 생성
        return merged.values()
                .stream()
                .sorted(
                        java.util.Comparator
                                .comparingDouble(
                                        MutableFusedResult::score
                                )
                                .reversed()
                                .thenComparing(
                                        MutableFusedResult::chunkId
                                )
                )
                .limit(limit)
                .map(MutableFusedResult::toResult)
                .toList();
    }

    private double calculateScore(
            int k,
            int rank
    ) {
        // RRF 점수 계산
        return 1.0 / (k + rank);
    }

    public record RankedResult(
            String chunkId,
            int rank
    ) {
    }

    public record FusedResult(
            String chunkId,
            Integer keywordRank,
            Integer vectorRank,
            double rrfScore
    ) {
    }

    private static final class MutableFusedResult {

        private final String chunkId;
        private Integer keywordRank;
        private Integer vectorRank;
        private double score;

        private MutableFusedResult(
                String chunkId
        ) {
            this.chunkId = chunkId;
        }

        private String chunkId() {
            return chunkId;
        }

        private double score() {
            return score;
        }

        private FusedResult toResult() {
            return new FusedResult(
                    chunkId,
                    keywordRank,
                    vectorRank,
                    score
            );
        }
    }
}