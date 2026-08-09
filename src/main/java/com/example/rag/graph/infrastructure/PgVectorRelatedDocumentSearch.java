package com.example.rag.graph.infrastructure;

import java.util.List;

import com.example.rag.graph.application.RelatedDocumentSearch;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PgVectorRelatedDocumentSearch
        implements RelatedDocumentSearch {

    private static final String FILE_NAME_METADATA_KEY = "file_name";

    private final VectorStore vectorStore;

    @Override
    public List<Document> search(
            String question,
            List<String> documentIds,
            int topK,
            double similarityThreshold
    ) {
        if (documentIds == null || documentIds.isEmpty()) {
            return List.of();
        }

        // 관련 문서 ID 메타데이터 필터 생성
        FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();

        // 관련 문서 범위 검색 요청 생성
        SearchRequest request =
                SearchRequest.builder()
                        .query(question)
                        .topK(topK)
                        .similarityThreshold(similarityThreshold)
                        .filterExpression(filterBuilder.in(FILE_NAME_METADATA_KEY, documentIds.toArray()).build())
                        .build();

        // 관련 문서 범위 벡터 검색
        return vectorStore.similaritySearch(request);
    }
}