package com.example.rag.document.application;

import java.util.List;

import com.example.rag.document.application.model.ChunkingResult;
import com.example.rag.document.application.model.ChunkingScenario;
import com.example.rag.document.application.model.DocumentProcessingResult;
import com.example.rag.document.infrastructure.pdf.PdfDocumentLoader;
import com.example.rag.document.infrastructure.splitter.TokenDocumentSplitter;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocumentChunkingService {

    private final PdfDocumentLoader pdfDocumentLoader;
    private final TokenDocumentSplitter tokenDocumentSplitter;

    public DocumentProcessingResult process() {
        // 페이지 단위 Document 생성
        List<Document> pageDocuments = pdfDocumentLoader.loadByPage();

        // 전체 페이지 통합 Document 생성
        List<Document> sourceDocuments = pdfDocumentLoader.loadAllPages();

        // 청킹 조건별 결과 생성
        List<ChunkingResult> chunkingResults =
                ChunkingScenario.comparisonCases()
                        .stream()
                        .map(scenario -> createResult(
                                sourceDocuments,
                                scenario
                        ))
                        .toList();

        // 전체 문서 처리 결과 생성
        return new DocumentProcessingResult(
                pageDocuments,
                chunkingResults
        );
    }

    public List<Document> createChunks(
            ChunkingScenario scenario
    ) {
        // 전체 페이지 통합 Document 생성
        List<Document> sourceDocuments =
                pdfDocumentLoader.loadAllPages();

        // 지정 조건 기반 문서 청킹
        return tokenDocumentSplitter.split(
                sourceDocuments,
                scenario
        );
    }

    private ChunkingResult createResult(
            List<Document> sourceDocuments,
            ChunkingScenario scenario
    ) {
        // 실험 조건에 따른 문서 청킹
        List<Document> chunks =
                tokenDocumentSplitter.split(
                        sourceDocuments,
                        scenario
                );

        // 청크 통계 결과 생성
        return ChunkingResult.from(scenario, chunks);
    }
}