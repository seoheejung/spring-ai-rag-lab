package com.example.rag.document.application.model;

import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Objects;

import org.springframework.ai.document.Document;

public record ChunkingResult(
        ChunkingScenario scenario,
        List<Document> chunks,
        int minChars,
        int maxChars,
        double averageChars
) {

    public ChunkingResult {
        // 외부 변경 방지를 위한 불변 목록 생성
        chunks = List.copyOf(chunks);
    }

    public static ChunkingResult from(
            ChunkingScenario scenario,
            List<Document> chunks
    ) {
        // 청크별 본문 문자 수 통계 계산
        IntSummaryStatistics statistics =
                chunks.stream()
                        .mapToInt(document ->
                                Objects.requireNonNullElse(
                                        document.getText(),
                                        ""
                                ).length()
                        )
                        .summaryStatistics();

        // 빈 청크 목록의 최소값 처리
        int minChars = chunks.isEmpty()
                ? 0
                : statistics.getMin();

        // 빈 청크 목록의 최대값 처리
        int maxChars = chunks.isEmpty()
                ? 0
                : statistics.getMax();

        // 청킹 조건과 문자 수 통계 생성
        return new ChunkingResult(
                scenario,
                chunks,
                minChars,
                maxChars,
                statistics.getAverage()
        );
    }

    public int chunkCount() {
        // 생성된 전체 청크 수 반환
        return chunks.size();
    }
}