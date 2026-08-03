package com.example.rag.generation.presentation.cli;

import java.util.List;
import java.util.Objects;

import com.example.rag.generation.application.RagService;
import com.example.rag.generation.config.RagProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "rag.generation",
        name = "enabled",
        havingValue = "true"
)
public class RagRunner implements ApplicationRunner {

    private final RagProperties properties;
    private final RagService ragService;

    @Override
    public void run(ApplicationArguments args) {
        // 수동 Retrieval 실행
        List<Document> results =
                ragService.retrieve(
                        properties.question(),
                        properties.topK(),
                        properties.similarityThreshold()
                );

        // 검색 결과 출력
        printSearchResults(results);

        // 수동 Context 출력
        printContext(results);

        // 수동 RAG 답변 생성
        String manualAnswer =
                ragService.answerManually(
                        properties.question(),
                        results
                );

        // Advisor 답변 생성
        String advisorAnswer =
                ragService.answerWithAdvisor(
                        properties.question(),
                        properties.topK(),
                        properties.similarityThreshold()
                );

        // 방식별 답변 출력
        printAnswers(manualAnswer, advisorAnswer);
    }

    private void printSearchResults(
            List<Document> results
    ) {
        System.out.println();
        System.out.println("===== 공통 검색 조건 =====");
        System.out.println("질문: " + properties.question());
        System.out.println("Top-K: " + properties.topK());
        System.out.println("Similarity Threshold: " + properties.similarityThreshold());
        System.out.println("검색 결과 수: " + results.size());

        // 검색 순위별 문서 출력
        for (int index = 0; index < results.size(); index++) {
            Document document = results.get(index);
            String text = Objects.requireNonNullElse(document.getText(), "");

            System.out.println();
            System.out.printf("[검색 결과 %d]%n", index + 1);
            System.out.println("문서: " + document.getMetadata().get("file_name"));
            System.out.println("청크: " + document.getMetadata().get("chunk_index"));
            System.out.println(
                    "저장된 페이지 범위: "
                            + document.getMetadata()
                            .get("page_number")
                            + "-"
                            + document.getMetadata()
                            .get("end_page_number")
            );
            System.out.println("유사도: " + document.getScore());
            System.out.println("본문:");
            System.out.println(text);
        }
    }

    private void printContext(
            List<Document> results
    ) {
        // 수동 Context 생성
        String context =
                ragService.createContext(results);

        System.out.println();
        System.out.println("===== 수동 RAG Context =====");
        System.out.println(context);
    }

    private void printAnswers(
            String manualAnswer,
            String advisorAnswer
    ) {
        System.out.println();
        System.out.println("===== 수동 RAG 답변 =====");
        System.out.println(manualAnswer);

        System.out.println();
        System.out.println("===== QuestionAnswerAdvisor 답변 =====");
        System.out.println(advisorAnswer);
    }
}