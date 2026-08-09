package com.example.rag.graph.presentation.cli;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.example.rag.evaluation.application.RetrievalMetricCalculator;
import com.example.rag.evaluation.application.model.EvaluationQuestion;
import com.example.rag.evaluation.application.model.RetrievalMetrics;
import com.example.rag.evaluation.application.model.RetrievedChunk;
import com.example.rag.evaluation.infrastructure.json.EvaluationQuestionLoader;
import com.example.rag.graph.application.GraphRagService;
import com.example.rag.graph.application.model.GraphQuestion;
import com.example.rag.graph.application.model.GraphRagCaseResult;
import com.example.rag.graph.application.model.GraphRagComparison;
import com.example.rag.graph.application.model.GraphRetrievalResult;
import com.example.rag.graph.config.GraphRagProperties;
import com.example.rag.graph.infrastructure.json.GraphQuestionLoader;
import com.example.rag.graph.infrastructure.json.GraphRagResultWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "rag.graph",
        name = "enabled",
        havingValue = "true"
)
public class GraphRagRunner implements ApplicationRunner {

    private final GraphRagProperties properties;
    private final EvaluationQuestionLoader evaluationQuestionLoader;
    private final GraphQuestionLoader graphQuestionLoader;
    private final GraphRagService graphRagService;
    private final RetrievalMetricCalculator metricCalculator;
    private final GraphRagResultWriter resultWriter;

    @Override
    public void run(ApplicationArguments args) {
        // Phase 4 평가 질문 조회
        List<EvaluationQuestion> evaluationQuestions = evaluationQuestionLoader.load(properties.questionsPath());

        // Graph 질문 메타데이터 조회
        List<GraphQuestion> graphQuestions =
                graphQuestionLoader.load(properties.graphQuestionsPath(), properties.questionsPath());

        // 평가 질문 ID 인덱스 생성
        Map<String, EvaluationQuestion> evaluationQuestionMap =
                evaluationQuestions.stream().collect(Collectors.toMap(EvaluationQuestion::id, Function.identity()));

        // 질문별 Graph RAG 비교 실행
        List<GraphRagComparison.CaseResult> results =
                graphQuestions.stream()
                        .map(graphQuestion -> executeQuestion(graphQuestion, evaluationQuestionMap))
                        .toList();

        // 전체 비교 결과 생성
        GraphRagComparison comparison = new GraphRagComparison(properties.topK(), properties.similarityThreshold(), results);

        // 비교 결과 출력
        printComparison(comparison);

        // 비교 결과 JSON 저장
        resultWriter.write(properties.resultsPath(), comparison);

        System.out.println();
        System.out.println("결과 파일: " + properties.resultsPath());
    }

    private GraphRagComparison.CaseResult executeQuestion(
            GraphQuestion graphQuestion,
            Map<String, EvaluationQuestion> evaluationQuestionMap
    ) {
        // Phase 4 평가 질문 조회
        EvaluationQuestion evaluationQuestion =
                Objects.requireNonNull(
                        evaluationQuestionMap.get(graphQuestion.questionId()),
                        () -> "평가 질문을 찾을 수 없습니다: " + graphQuestion.questionId()
                );

        // 일반 RAG와 Graph 기반 RAG 실행
        GraphRagCaseResult ragResult = graphRagService.execute(evaluationQuestion.question(), graphQuestion);
        GraphRetrievalResult retrievalResult = ragResult.retrievalResult();

        // 일반 RAG 검색 결과 변환
        List<RetrievedChunk> generalRetrievedChunks =
                createRetrievedChunks(retrievalResult.vectorDocuments(), evaluationQuestion.expectedChunkIds());

        // 관련 문서 범위 검색 결과 변환
        List<RetrievedChunk> graphDocumentRetrievedChunks =
                createRetrievedChunks(retrievalResult.graphDocuments(), evaluationQuestion.expectedChunkIds());

        // Graph 기반 병합 결과 변환
        List<RetrievedChunk> mergedRetrievedChunks =
                createRetrievedChunks(retrievalResult.mergedDocuments(), evaluationQuestion.expectedChunkIds());

        // 일반 RAG 검색 지표 계산
        RetrievalMetrics generalMetrics =
                metricCalculator.calculate(
                        evaluationQuestion.answerable(),
                        evaluationQuestion.expectedChunkIds(),
                        generalRetrievedChunks
                );

        // Graph 기반 RAG 검색 지표 계산
        RetrievalMetrics graphMetrics =
                metricCalculator.calculate(
                        evaluationQuestion.answerable(),
                        evaluationQuestion.expectedChunkIds(),
                        mergedRetrievedChunks
                );

        // 질문별 비교 결과 생성
        return new GraphRagComparison.CaseResult(
                evaluationQuestion.id(),
                evaluationQuestion.question(),
                evaluationQuestion.type(),
                evaluationQuestion.expectedDocumentIds(),
                evaluationQuestion.expectedChunkIds(),
                retrievalResult.graphEntities(),
                retrievalResult.graphQueryType(),
                retrievalResult.graphRelationships(),
                retrievalResult.graphRelationships().size(),
                retrievalResult.relatedDocumentIds(),
                generalRetrievedChunks,
                graphDocumentRetrievedChunks,
                mergedRetrievedChunks,
                generalMetrics,
                graphMetrics,
                ragResult.generalAnswer(),
                ragResult.graphAnswer(),
                retrievalResult.status(),
                null
        );
    }

    private List<RetrievedChunk> createRetrievedChunks(List<Document> documents, List<String> expectedChunkIds) {
        Set<String> expectedChunkIdSet = Set.copyOf(expectedChunkIds);

        return IntStream
                .range(0, documents.size())
                .mapToObj(index -> createRetrievedChunk(documents.get(index), index + 1, expectedChunkIdSet))
                .toList();
    }

    private RetrievedChunk createRetrievedChunk(Document document, int rank, Set<String> expectedChunkIds) {
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
                Objects.requireNonNullElse(document.getText(), "")
        );
    }

    private void printComparison(GraphRagComparison comparison) {
        System.out.println();
        System.out.println("===== GraphDB와 Graph 기반 RAG 비교 =====");

        for (GraphRagComparison.CaseResult result : comparison.results()) {
            printCase(comparison.topK(), comparison.similarityThreshold(), result);
        }
    }

    private void printCase(int topK, double similarityThreshold, GraphRagComparison.CaseResult result) {
        System.out.println();
        System.out.println("----------------------------------------");
        System.out.println("질문 ID: " + result.questionId());
        System.out.println("질문: " + result.question());
        System.out.println("Top-K: " + topK);
        System.out.println("Similarity Threshold: " + similarityThreshold);
        System.out.println("상태: " + result.status());
        System.out.println();
        System.out.println("===== 일반 RAG =====");
        System.out.println("검색 결과 수: " + result.generalRetrievedChunks().size());

        printChunks(result.generalRetrievedChunks());
        printMetrics(topK, result.generalRetrievalMetrics());

        System.out.println("답변:" + System.lineSeparator() + result.generalAnswer());
        System.out.println();
        System.out.println("===== Graph 탐색 =====");
        System.out.println("엔티티: " + formatList(result.graphEntities()));
        System.out.println("탐색 유형: " + result.graphQueryType());
        System.out.println("Hop 수: " + result.graphHopCount());
        System.out.println("경로: " + formatList(result.graphRelationships()));
        System.out.println("관련 문서 ID: " + formatList(result.relatedDocumentIds()));
        System.out.println();
        System.out.println("===== 관련 문서 범위 검색 =====");
        System.out.println("검색 결과 수: " + result.graphDocumentRetrievedChunks().size());

        printChunks(result.graphDocumentRetrievedChunks());

        System.out.println();
        System.out.println("===== Graph 기반 RAG =====");
        System.out.println("병합 결과 수: " + result.mergedRetrievedChunks().size());

        printChunks(result.mergedRetrievedChunks());
        printMetrics(topK, result.graphRetrievalMetrics());
        System.out.println("답변:" + System.lineSeparator() + result.graphAnswer());
    }

    private void printChunks(List<RetrievedChunk> chunks) {
        if (chunks.isEmpty()) {
            System.out.println("검색 청크: 없음");
            return;
        }

        System.out.println("검색 청크:");

        for (RetrievedChunk chunk : chunks) {
            System.out.printf(
                    "  %d. %s | score=%s | relevant=%s%n",
                    chunk.rank(),
                    chunk.chunkId(),
                    chunk.score(),
                    chunk.relevant()
            );
        }
    }

    private void printMetrics(int topK, RetrievalMetrics metrics) {
        System.out.printf("Hit@%d: %.4f%n", topK, metrics.hitAtK().doubleValue());
        System.out.printf("Recall@%d: %.4f%n", topK, metrics.recallAtK());
        System.out.printf("Precision@%d: %.4f%n", topK, metrics.precisionAtK());
        System.out.printf("Reciprocal Rank: %.4f%n", metrics.reciprocalRank());
    }

    private String formatList(List<String> values) {
        if (values.isEmpty()) {
            return "없음";
        }

        return values.toString();
    }
}