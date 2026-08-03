package com.example.rag.document.infrastructure.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PdfDocumentLoaderTest {

    @Autowired
    private PdfDocumentLoader pdfDocumentLoader;

    @Test
    void PDF를_페이지별_Document로_읽는다() {
        // 페이지 단위 PDF 읽기
        List<Document> documents = pdfDocumentLoader.loadByPage();

        // 페이지 Document 생성 여부 검증
        assertThat(documents).isNotEmpty();

        // Document 필수 정보 검증
        for (int index = 0; index < documents.size(); index++) {
            Document document = documents.get(index);

            assertThat(document.getId())
                    .as("Document %d ID", index + 1)
                    .isNotBlank();

            assertThat(document.getText())
                    .as(
                            "Document %d 본문, metadata=%s",
                            index + 1,
                            document.getMetadata()
                    )
                    .isNotBlank();

            assertThat(document.getMetadata())
                    .as("Document %d 메타데이터", index + 1)
                    .containsKeys(
                            "file_name",
                            "page_number"
                    )
                    .doesNotContainKey("end_page_number");
        }
    }

    @Test
    void PDF_전체_페이지를_하나의_Document로_읽는다() {
        // 전체 페이지 통합 읽기
        List<Document> documents = pdfDocumentLoader.loadAllPages();

        // 통합 Document 수 검증
        assertThat(documents).hasSize(1);

        // 통합 본문 생성 여부 검증
        assertThat(documents.getFirst().getText()).isNotBlank();
    }
}