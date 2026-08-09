package com.example.rag.graph.application;

import java.util.List;

import com.example.rag.generation.application.RagService;
import com.example.rag.graph.application.model.GraphPathResult;
import com.example.rag.graph.application.model.GraphQuestion;
import com.example.rag.graph.application.model.GraphRagCaseResult;
import com.example.rag.graph.application.model.GraphRetrievalResult;
import com.example.rag.graph.config.GraphRagProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GraphRagService {

    private final RagService ragService;
    private final GraphSearchService graphSearchService;
    private final RelatedDocumentSearch relatedDocumentSearch;
    private final GraphDocumentMerger graphDocumentMerger;
    private final GraphRagProperties graphRagProperties;

    public GraphRagCaseResult execute(
            String question,
            GraphQuestion graphQuestion
    ) {
        // Graph RAG 검색 조건 조회
        int topK = graphRagProperties.topK();
        double similarityThreshold = graphRagProperties.similarityThreshold();

        // 기존 전체 Vector Store 검색
        List<Document> vectorDocuments = ragService.retrieve(question, topK, similarityThreshold);

        // 기존 일반 RAG 답변 생성
        String generalAnswer = ragService.answerManually(question, vectorDocuments);

        // Graph 관계 탐색
        GraphPathResult graphPath = graphSearchService.search(graphQuestion.questionId());

        // 관련 문서 범위 Vector Store 검색
        List<Document> graphDocuments =
                searchRelatedDocuments(
                        question,
                        graphPath,
                        topK,
                        similarityThreshold
                );

        // 기존 벡터 검색과 Graph 문서 검색 결과 병합
        List<Document> mergedDocuments = graphDocumentMerger.merge(vectorDocuments, graphDocuments, topK);

        // Graph 기반 RAG 답변 생성
        String graphAnswer = ragService.answerManually(question, mergedDocuments);

        // Graph Retrieval 상태 결정
        GraphRetrievalResult.Status status = determineStatus(graphPath, graphDocuments);

        // Graph Retrieval 결과 생성
        GraphRetrievalResult retrievalResult =
                new GraphRetrievalResult(
                        graphQuestion.questionId(),
                        question,
                        graphQuestion.entityNames(),
                        graphQuestion.queryType(),
                        graphPath.relationships(),
                        graphPath.documentIds(),
                        vectorDocuments,
                        graphDocuments,
                        mergedDocuments,
                        status
                );

        // 일반 RAG와 Graph RAG 비교 결과 생성
        return new GraphRagCaseResult(
                graphQuestion.questionId(),
                question,
                retrievalResult,
                generalAnswer,
                graphAnswer
        );
    }

    private List<Document> searchRelatedDocuments(
            String question,
            GraphPathResult graphPath,
            int topK,
            double similarityThreshold
    ) {
        // Graph 관련 문서 없음 처리
        if (graphPath.documentIds().isEmpty()) {
            return List.of();
        }

        // 관련 문서 범위 벡터 검색
        return relatedDocumentSearch.search(
                question,
                graphPath.documentIds(),
                topK,
                similarityThreshold
        );
    }

    private GraphRetrievalResult.Status determineStatus(
            GraphPathResult graphPath,
            List<Document> graphDocuments
    ) {
        // Graph 경로 없음 판정
        if (graphPath.documentIds().isEmpty()) {
            return GraphRetrievalResult.Status.GRAPH_PATH_NOT_FOUND;
        }

        // 관련 문서 검색 결과 없음 판정
        if (graphDocuments.isEmpty()) {
            return GraphRetrievalResult.Status.GRAPH_DOCUMENT_SEARCH_EMPTY;
        }

        // Graph Retrieval 성공 판정
        return GraphRetrievalResult.Status.SUCCESS;
    }
}