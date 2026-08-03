package com.example.rag.generation.application;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RagService {

    private static final String SYSTEM_PROMPT = """
            제공된 문서만 사용하여 답변하십시오.
            문서에 근거가 없으면 알 수 없다고 답변하십시오.
            답변에 사용한 문서명과 저장된 페이지 범위를 표시하십시오.
            """;

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public List<Document> retrieve(
            String question,
            int topK,
            double similarityThreshold
    ) {
        // 검색 조건 생성
        SearchRequest request =
                SearchRequest.builder()
                        .query(question)
                        .topK(topK)
                        .similarityThreshold(similarityThreshold)
                        .build();

        // 질문 기반 문서 검색
        return vectorStore.similaritySearch(request);
    }

    public String createContext(
            List<Document> results
    ) {
        // 검색 문서 Context 생성
        return results.stream()
                .map(this::createDocumentContext)
                .collect(Collectors.joining("\n\n"));
    }

    public String answerManually(
            String question,
            List<Document> results
    ) {
        // 검색 문서 Context 생성
        String context = createContext(results);

        // 수동 RAG 답변 생성
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user("""
                        질문:
                        %s

                        검색 문서:
                        %s
                        """.formatted(
                        question,
                        context
                ))
                .call()
                .content();
    }

    public String answerWithAdvisor(
            String question,
            int topK,
            double similarityThreshold
    ) {
        // Advisor 검색 조건 생성
        SearchRequest request =
                SearchRequest.builder()
                        .topK(topK)
                        .similarityThreshold(similarityThreshold)
                        .build();

        // QuestionAnswerAdvisor 생성
        QuestionAnswerAdvisor advisor =
                QuestionAnswerAdvisor.builder(vectorStore)
                        .searchRequest(request)
                        .build();

        // Advisor 기반 답변 생성
        return chatClient.prompt()
                .user(question)
                .advisors(advisor)
                .call()
                .content();
    }

    private String createDocumentContext(
            Document document
    ) {
        // 문서 메타데이터 조회
        Object fileName = document.getMetadata().get("file_name");
        Object pageNumber = document.getMetadata().get("page_number");
        Object endPageNumber = document.getMetadata().get("end_page_number");
        Object chunkIndex = document.getMetadata().get("chunk_index");
        String text = Objects.requireNonNullElse(document.getText(), "");

        // 문서별 Context 생성
        return """
                [문서]
                문서명: %s
                저장된 페이지 범위: %s-%s
                청크 번호: %s
                내용:
                %s
                """.formatted(
                fileName,
                pageNumber,
                endPageNumber,
                chunkIndex,
                text
        );
    }
}