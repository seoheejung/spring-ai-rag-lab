package com.example.rag.retrieval.presentation.cli;

import java.util.List;
import java.util.Objects;

import com.example.rag.retrieval.application.VectorSearchService;
import com.example.rag.retrieval.config.VectorSearchProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "rag.retrieval",
        name = "enabled",
        havingValue = "true"
)
public class VectorSearchRunner implements ApplicationRunner {

    private final VectorSearchProperties properties;
    private final VectorSearchService vectorSearchService;
    private final EmbeddingModel embeddingModel;

    @Override
    public void run(ApplicationArguments args) {
        // 임베딩 모델 정보 출력
        printEmbeddingModelInfo();

        if (properties.store()) {
            storeDocuments();
        }

        if (properties.search()) {
            searchDocuments();
        }
    }

    private void printEmbeddingModelInfo() {
        System.out.println();
        System.out.println("===== 임베딩 모델 정보 =====");
        System.out.println("EmbeddingModel: " + embeddingModel.getClass().getSimpleName());
        System.out.println("Embedding dimensions: " + embeddingModel.dimensions());
    }

    private void storeDocuments() {
        // 문서 청크 저장
        int storedChunkCount = vectorSearchService.store();

        System.out.println();
        System.out.println("===== 문서 벡터 저장 결과 =====");
        System.out.println("저장 요청 청크 수: " + storedChunkCount);
    }

    private void searchDocuments() {
        // 질문 기반 유사 문서 검색
        List<Document> results =
                vectorSearchService.search(
                        properties.question(),
                        properties.topK(),
                        properties.similarityThreshold()
                );

        printSearchResults(results);
    }

    private void printSearchResults(
            List<Document> results
    ) {
        System.out.println();
        System.out.println("===== 벡터 검색 결과 =====");
        System.out.println("질문: " + properties.question());
        System.out.println("Top-K: " + properties.topK());
        System.out.println("Similarity Threshold: " + properties.similarityThreshold());
        System.out.println("검색 결과 수: " + results.size());

        // 검색 순위별 결과 출력
        for (int index = 0; index < results.size(); index++) {
            Document document = results.get(index);
            String text = Objects.requireNonNullElse(
                    document.getText(),
                    ""
            );

            System.out.println();
            System.out.printf("[검색 결과 %d]%n", index + 1);
            System.out.println("ID: " + document.getId());
            System.out.println("문서: " + document.getMetadata().get("file_name"));
            System.out.println("청크: " + document.getMetadata().get("chunk_index"));
            System.out.println("유사도: " + document.getScore());
            System.out.println("Metadata: " + document.getMetadata());
            System.out.println("본문:");
            System.out.println(text);
        }
    }
}