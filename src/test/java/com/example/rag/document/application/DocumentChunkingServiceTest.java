package com.example.rag.document.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.rag.document.application.model.DocumentProcessingResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DocumentChunkingServiceTest {

    @Autowired
    private DocumentChunkingService documentChunkingService;

    @Test
    void 문서를_읽고_모든_청킹_조건을_실행한다() {
        // 전체 문서 처리 흐름 실행
        DocumentProcessingResult result = documentChunkingService.process();

        // 페이지 Document 생성 여부 검증
        assertThat(result.pageDocuments()).isNotEmpty();

        // 전체 청킹 실험 실행 여부 검증
        assertThat(result.chunkingResults()).hasSize(5);

        // 실험별 청크 생성과 청크 수 일치 검증
        assertThat(result.chunkingResults())
                .allSatisfy(chunkingResult -> {
                    assertThat(chunkingResult.chunks())
                            .isNotEmpty();
                    assertThat(chunkingResult.chunkCount())
                            .isEqualTo(
                                    chunkingResult.chunks().size()
                            );
                });
    }
}