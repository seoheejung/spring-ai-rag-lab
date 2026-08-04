package com.example.rag.evaluation.application;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;

import com.example.rag.evaluation.application.model.EvaluationCaseResult;
import com.example.rag.evaluation.application.model.EvaluationQuestion;
import com.example.rag.evaluation.application.model.EvaluationSummary;
import com.example.rag.evaluation.application.model.RetrievalMetrics;
import com.example.rag.evaluation.application.model.RetrievedChunk;
import com.example.rag.generation.application.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final RagService ragService;
    private final RetrievalMetricCalculator metricCalculator;

    public EvaluationSummary evaluate(
            List<EvaluationQuestion> questions,
            int topK,
            double similarityThreshold
    ) {
        // 질문별 검색·생성 평가 실행
        List<EvaluationCaseResult> results = questions.stream()
                .map(question
                        -> evaluateQuestion(question, topK, similarityThreshold)
                )
                .toList();

        // 전체 검색 지표 요약 생성
        return metricCalculator.summarize(
                topK,
                similarityThreshold,
                results
        );
    }

    private EvaluationCaseResult evaluateQuestion(
            EvaluationQuestion question,
            int topK,
            double similarityThreshold
    ) {
        // Phase 3 수동 Retrieval 실행
        List<Document> documents = ragService.retrieve(
                question.question(),
                topK,
                similarityThreshold
        );

        // 검색 결과 평가 모델 변환
        List<RetrievedChunk> retrievedChunks =
                createRetrievedChunks(documents, question.expectedChunkIds());

        // 질문별 검색 지표 계산
        RetrievalMetrics retrievalMetrics =
                metricCalculator.calculate(
                        question.answerable(),
                        question.expectedChunkIds(),
                        retrievedChunks
                );

        // Phase 3 수동 RAG 답변 생성
        String answer = ragService.answerManually(question.question(), documents);

        // 수동 검토 전 결과 생성
        return EvaluationCaseResult.unreviewed(
                question,
                retrievedChunks,
                retrievalMetrics,
                answer
        );
    }

    private List<RetrievedChunk> createRetrievedChunks(
            List<Document> documents,
            List<String> expectedChunkIds
    ) {
        Set<String> expectedChunkIdSet =
                Set.copyOf(expectedChunkIds);

        return IntStream
                .range(0, documents.size())
                .mapToObj(index ->
                        createRetrievedChunk(
                                documents.get(index),
                                index + 1,
                                expectedChunkIdSet
                        )
                )
                .toList();
    }

    private RetrievedChunk createRetrievedChunk(
            Document document,
            int rank,
            Set<String> expectedChunkIds
    ) {
        Map<String, Object> metadata = document.getMetadata();

        String documentId = Objects.toString(metadata.get("file_name"), "");

        String chunkIndex = Objects.toString(metadata.get("chunk_index"), "");

        // 평가용 청크 ID 생성
        String chunkId = documentId + "#" + chunkIndex;

        return new RetrievedChunk(
                rank,
                documentId,
                chunkId,
                document.getScore(),
                expectedChunkIds.contains(chunkId),
                metadata,
                Objects.requireNonNullElse(
                        document.getText(),
                        ""
                )
        );
    }
}