package com.example.rag.document.infrastructure.splitter;

import java.util.List;

import com.example.rag.document.application.model.ChunkingScenario;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

@Component
public class TokenDocumentSplitter {

    // 모든 실험에 적용할 고정 청킹 조건
    private static final int MIN_CHUNK_SIZE_CHARS = 350;
    private static final int MIN_CHUNK_LENGTH_TO_EMBED = 5;
    private static final int MAX_NUM_CHUNKS = 5000;

    public List<Document> split(
            List<Document> sourceDocuments,
            ChunkingScenario scenario
    ) {
        // 실험 조건에 따른 TokenTextSplitter 생성
        TokenTextSplitter splitter =
                TokenTextSplitter.builder()
                        .withChunkSize(scenario.chunkSize())
                        .withMinChunkSizeChars(MIN_CHUNK_SIZE_CHARS)
                        .withMinChunkLengthToEmbed(MIN_CHUNK_LENGTH_TO_EMBED)
                        .withMaxNumChunks(MAX_NUM_CHUNKS)
                        .withKeepSeparator(
                                scenario.keepSeparator()
                        )
                        .build();

        // 원본 Document 목록 청킹
        return splitter.apply(sourceDocuments);
    }
}