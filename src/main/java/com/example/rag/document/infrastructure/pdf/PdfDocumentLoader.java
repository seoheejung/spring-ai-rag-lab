package com.example.rag.document.infrastructure.pdf;

import java.util.List;

import com.example.rag.document.config.DocumentProcessingProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PdfDocumentLoader {

    private final DocumentProcessingProperties properties;

    // 페이지 단위 읽기
    public List<Document> loadByPage() {
        // PDF Resource 생성
        Resource pdfResource = createPdfResource();

        // 원본 PDF 상태 검증
        validateResource(pdfResource);

        // 페이지 단위 문서 읽기 설정
        PdfDocumentReaderConfig config =
                PdfDocumentReaderConfig.builder()
                        .withPagesPerDocument(1)
                        .withPageTopMargin(0)
                        .withPageBottomMargin(0)
                        .build();

        PagePdfDocumentReader reader = new PagePdfDocumentReader(pdfResource, config);

        // 페이지별 Document 생성
        return reader.read();
    }

    // 전체 페이지 통합 읽기
    public List<Document> loadAllPages() {
        // PDF Resource 생성
        Resource pdfResource = createPdfResource();

        // 원본 PDF 상태 검증
        validateResource(pdfResource);

        // 전체 페이지 통합 읽기 설정
        PdfDocumentReaderConfig config =
                PdfDocumentReaderConfig.builder()
                        .withPagesPerDocument(PdfDocumentReaderConfig.ALL_PAGES)
                        .withPageTopMargin(0)
                        .withPageBottomMargin(0)
                        .build();

        PagePdfDocumentReader reader = new PagePdfDocumentReader(pdfResource, config);

        // 통합 Document 생성
        return reader.read();
    }

    private Resource createPdfResource() {
        // 설정된 PDF 경로를 Resource로 변환
        return new FileSystemResource(properties.sourcePdf());
    }

    private void validateResource(Resource pdfResource) {
        // 원본 PDF 존재 여부 검증
        if (!pdfResource.exists()) {
            throw new IllegalStateException(
                    "원본 PDF를 찾을 수 없습니다: "
                            + pdfResource.getDescription()
            );
        }

        // 원본 PDF 읽기 가능 여부 검증
        if (!pdfResource.isReadable()) {
            throw new IllegalStateException(
                    "원본 PDF를 읽을 수 없습니다: "
                            + pdfResource.getDescription()
            );
        }
    }
}