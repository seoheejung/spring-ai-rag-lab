package com.example.rag.retrieval.infrastructure.postgres;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class KeywordSearchRepository {

    private static final String SEARCH_SQL = """
        SELECT
            metadata ->> 'file_name' AS "fileName",
            metadata ->> 'chunk_index' AS "chunkIndex",
            content,
            ts_rank_cd(
                to_tsvector('simple', COALESCE(content, '')),
                websearch_to_tsquery('simple', :question)
            ) AS "keywordScore"
        FROM public.vector_store
        WHERE to_tsvector(
                'simple',
                COALESCE(content, '')
              ) @@ websearch_to_tsquery(
                'simple',
                :question
              )
        ORDER BY
            "keywordScore" DESC,
            (metadata ->> 'chunk_index')::integer
        LIMIT :topK
        """;

    private final JdbcClient jdbcClient;

    public List<KeywordSearchRow> search(
            String question,
            int topK
    ) {
        // PostgreSQL 키워드 검색 실행
        return jdbcClient
                .sql(SEARCH_SQL)
                .param("question", question)
                .param("topK", topK)
                .query(KeywordSearchRow.class)
                .list();
    }

    public record KeywordSearchRow(
            String fileName,
            String chunkIndex,
            String content,
            double keywordScore
    ) {
    }
}