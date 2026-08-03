package com.example.rag.document.infrastructure.splitter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.example.rag.document.application.model.ChunkingScenario;
import com.example.rag.document.infrastructure.pdf.PdfDocumentLoader;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TokenDocumentSplitterTest {

    @Autowired
    private PdfDocumentLoader pdfDocumentLoader;

    @Autowired
    private TokenDocumentSplitter tokenDocumentSplitter;

    @Test
    void 청킹_후에도_원본_메타데이터를_유지한다() {
        // 전체 페이지 통합 Document 생성
        List<Document> sourceDocuments = pdfDocumentLoader.loadAllPages();

        // Chunk Size 800 실험 조건 생성
        ChunkingScenario scenario =
                new ChunkingScenario(
                        "B",
                        800,
                        true,
                        "Chunk Size 800"
                );

        // 통합 Document 청킹
        List<Document> chunks = tokenDocumentSplitter.split(sourceDocuments, scenario);

        // 청크 생성 여부 검증
        assertThat(chunks).isNotEmpty();

        // 청크 본문과 원본 메타데이터 유지 검증
        assertThat(chunks).allSatisfy(chunk -> {
            assertThat(chunk.getId()).isNotBlank();
            assertThat(chunk.getText()).isNotBlank();
            assertThat(chunk.getMetadata())
                    .containsKeys(
                            "file_name",
                            "page_number",
                            "end_page_number"
                    );
        });
    }
}