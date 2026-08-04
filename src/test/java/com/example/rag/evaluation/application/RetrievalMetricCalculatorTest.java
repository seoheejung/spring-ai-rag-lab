package com.example.rag.evaluation.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import com.example.rag.evaluation.application.model.RetrievalMetrics;
import com.example.rag.evaluation.application.model.RetrievedChunk;
import org.junit.jupiter.api.Test;

class RetrievalMetricCalculatorTest {

    private final RetrievalMetricCalculator calculator =
            new RetrievalMetricCalculator();

    @Test
    void 정답_청크의_순위와_개수로_검색_지표를_계산한다() {
        // 검색 결과 생성
        List<RetrievedChunk> retrievedChunks =
                List.of(
                        createChunk(1, "document.pdf#10"),
                        createChunk(2, "document.pdf#20"),
                        createChunk(3, "document.pdf#30"),
                        createChunk(4, "document.pdf#40"),
                        createChunk(5, "document.pdf#50")
                );

        // 기대 청크 생성
        List<String> expectedChunkIds = List.of("document.pdf#20", "document.pdf#60");

        // 검색 지표 계산
        RetrievalMetrics metrics = calculator.calculate(true, expectedChunkIds, retrievedChunks);

        // Hit@5 검증
        assertThat(metrics.hitAtK()).isEqualTo(1);

        // Recall@5 검증
        assertThat(metrics.recallAtK()).isEqualTo(0.5);

        // Precision@5 검증
        assertThat(metrics.precisionAtK()).isEqualTo(0.2);

        // Reciprocal Rank 검증
        assertThat(metrics.reciprocalRank()).isEqualTo(0.5);
    }

    @Test
    void 답이_없는_질문은_검색_지표에서_제외한다() {
        // 답이 없는 질문의 검색 지표 계산
        RetrievalMetrics metrics = calculator.calculate(false, List.of(), List.of());

        // 검색 지표 제외 여부 검증
        assertThat(metrics.hitAtK()).isNull();
        assertThat(metrics.recallAtK()).isNull();
        assertThat(metrics.precisionAtK()).isNull();
        assertThat(metrics.reciprocalRank()).isNull();
    }

    private RetrievedChunk createChunk(
            int rank,
            String chunkId
    ) {
        // 테스트용 검색 청크 생성
        return new RetrievedChunk(
                rank,
                "document.pdf",
                chunkId,
                0.9,
                false,
                Map.of(
                        "file_name",
                        "document.pdf",
                        "chunk_index",
                        rank
                ),
                "검색 청크 " + rank
        );
    }
}