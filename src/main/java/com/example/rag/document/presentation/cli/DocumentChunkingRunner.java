package com.example.rag.document.presentation.cli;

import java.util.List;
import java.util.Objects;

import com.example.rag.document.application.DocumentChunkingService;
import com.example.rag.document.application.model.ChunkingResult;
import com.example.rag.document.application.model.ChunkingScenario;
import com.example.rag.document.application.model.DocumentProcessingResult;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "rag.document",
        name = "enabled",
        havingValue = "true"
)
public class DocumentChunkingRunner implements ApplicationRunner {

    // 본문 시작·종료 출력 길이
    private static final int PREVIEW_LENGTH = 180;

    private final DocumentChunkingService documentChunkingService;

    @Override
    public void run(ApplicationArguments args) {
        // 문서 읽기와 청킹 실험 실행
        DocumentProcessingResult result = documentChunkingService.process();

        // 페이지 Document 출력
        printSourceDocuments(result.pageDocuments());

        // 청킹 실험 결과 출력
        printChunkingResults(result.chunkingResults());
    }

    private void printSourceDocuments(
            List<Document> documents
    ) {
        System.out.println();
        System.out.println("===== PDF 문서 읽기 결과 =====");
        System.out.println("페이지 Document 수: " + documents.size());

        // 페이지별 Document 정보 출력
        for (int index = 0; index < documents.size(); index++) {
            Document document = documents.get(index);
            String text = getText(document);

            System.out.println();
            System.out.printf("[Document %d]%n", index + 1);
            System.out.println("ID: " + document.getId());
            System.out.println("본문 문자 수: " + text.length());
            System.out.println("Metadata: " + document.getMetadata());
            System.out.println("본문 시작: " + createStartPreview(text));
            System.out.println("본문 끝: " + createEndPreview(text));
        }
    }

    private void printChunkingResults(
            List<ChunkingResult> results
    ) {
        System.out.println();
        System.out.println("===== 청킹 실험 결과 =====");

        // 실험 조건별 청킹 통계 출력
        for (ChunkingResult result : results) {
            ChunkingScenario scenario = result.scenario();

            System.out.println();
            System.out.printf(
                    "[실험 %s] %s%n",
                    scenario.code(),
                    scenario.description()
            );
            System.out.printf(
                    "chunkSize=%d, keepSeparator=%s%n",
                    scenario.chunkSize(),
                    scenario.keepSeparator()
            );
            System.out.println("청크 수: " + result.chunkCount());
            System.out.println("최소 문자 수: " + result.minChars());
            System.out.println("최대 문자 수: " + result.maxChars());
            System.out.printf("평균 문자 수: %.2f%n", result.averageChars());

            printChunks(result.chunks());
        }
    }

    private void printChunks(List<Document> chunks) {
        // 청크별 본문과 메타데이터 출력
        for (int index = 0; index < chunks.size(); index++) {
            Document chunk = chunks.get(index);
            String text = getText(chunk);

            System.out.println();
            System.out.printf("  [Chunk %d]%n", index + 1);
            System.out.println("  ID: " + chunk.getId());
            System.out.println("  문자 수: " + text.length());
            System.out.println("  Metadata: " + chunk.getMetadata());
            System.out.println("  시작: " + createStartPreview(text));
            System.out.println("  끝: " + createEndPreview(text));
        }
    }

    private String getText(Document document) {
        // null 본문을 빈 문자열로 변환
        return Objects.requireNonNullElse(
                document.getText(),
                ""
        );
    }

    private String createStartPreview(String text) {
        // 본문 시작 미리보기 생성
        String normalized = normalize(text);

        if (normalized.length() <= PREVIEW_LENGTH) {
            return normalized;
        }

        return normalized.substring(0, PREVIEW_LENGTH)
                + "...";
    }

    private String createEndPreview(String text) {
        // 본문 종료 미리보기 생성
        String normalized = normalize(text);

        if (normalized.length() <= PREVIEW_LENGTH) {
            return normalized;
        }

        return "..."
                + normalized.substring(
                normalized.length() - PREVIEW_LENGTH
        );
    }

    private String normalize(String text) {
        // 연속 공백과 줄바꿈 정규화
        return text
                .replaceAll("\\s+", " ")
                .strip();
    }
}