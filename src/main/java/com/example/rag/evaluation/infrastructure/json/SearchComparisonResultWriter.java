package com.example.rag.evaluation.infrastructure.json;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.example.rag.evaluation.application.SearchComparisonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class SearchComparisonResultWriter {

    private final JsonMapper jsonMapper;

    public void write(
            Path baselineResultsPath,
            Path resultsPath,
            SearchExecutionSettings settings,
            SearchComparisonService.SearchComparisonResult comparison,
            RerankingExecution reranking
    ) {
        try {
            // Phase 4 기준 결과 읽기
            JsonNode phase4Baseline = jsonMapper.readTree(Files.readString(baselineResultsPath));

            // 결과 저장 디렉터리 생성
            Path parentPath = resultsPath.getParent();

            if (parentPath != null) {
                Files.createDirectories(parentPath);
            }

            // Phase 5 저장 결과 생성
            Phase5SearchResult result =
                    new Phase5SearchResult(
                            phase4Baseline,
                            settings,
                            comparison,
                            reranking
                    );

            // Phase 5 검색 결과 저장
            jsonMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValue(
                            resultsPath.toFile(),
                            result
                    );
        }
        catch (IOException exception) {
            throw new IllegalStateException("Phase 5 검색 결과 저장에 실패했습니다: " + resultsPath, exception);
        }
    }

    public record Phase5SearchResult(
            JsonNode phase4Baseline,
            SearchExecutionSettings phase5Settings,
            SearchComparisonService.SearchComparisonResult comparison,
            RerankingExecution reranking
    ) {
    }

    public record SearchExecutionSettings(
            int questionCount,
            int keywordTopK,
            int vectorTopK,
            double similarityThreshold,
            int rrfK,
            int candidateCount,
            int finalTopK
    ) {
    }

    public record RerankingExecution(
            String status,
            String reason,
            int candidateCount
    ) {
    }
}