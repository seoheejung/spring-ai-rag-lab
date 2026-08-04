package com.example.rag.retrieval.application;

import java.util.List;

import com.example.rag.retrieval.infrastructure.postgres.KeywordSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KeywordSearchService {

    private final KeywordSearchRepository repository;

    public List<KeywordSearchRepository.KeywordSearchRow> search(
            String question,
            int topK
    ) {
        // 검색 질문 검증
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("검색 질문이 비어 있습니다.");
        }

        // 키워드 검색 결과 수 검증
        if (topK <= 0) {
            throw new IllegalArgumentException("키워드 검색 Top-K는 1 이상이어야 합니다.");
        }

        // PostgreSQL 키워드 검색 실행
        return repository.search(question, topK);
    }
}