package com.example.rag.graph.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

class GraphDocumentMergerTest {

    private final GraphDocumentMerger merger = new GraphDocumentMerger();

    @Test
    void 기존_순서를_유지하고_중복을_제거한_뒤_Graph_청크를_추가한다() {
        Document vector10 = document(10, "vector-10");
        Document vector20 = document(20, "vector-20");
        Document graph20 = document(20, "graph-20");
        Document graph30 = document(30, "graph-30");

        List<Document> result =
                merger.merge(List.of(vector10, vector20), List.of(graph20, graph30), 5);

        assertThat(result).containsExactly(vector10, vector20, graph30);

        // 기존 벡터 청크 우선 유지
        assertThat(result.get(1)).isSameAs(vector20);

        // 기존 본문 유지
        assertThat(result.get(1).getText()).isEqualTo("vector-20");

        // 기존 메타데이터 유지
        assertThat(result.get(1).getMetadata())
                .containsEntry("file_name", "document.pdf")
                .containsEntry("chunk_index", 20);
    }

    @Test
    void 최종_청크_수를_limit으로_제한한다() {
        Document vector10 = document(10, "vector-10");
        Document vector20 = document(20, "vector-20");
        Document graph30 = document(30, "graph-30");

        List<Document> result =
                merger.merge(List.of(vector10, vector20), List.of(graph30), 2);

        assertThat(result).containsExactly(vector10, vector20);
        assertThat(result).hasSize(2);
    }

    private Document document(int chunkIndex, String text) {
        return Document.builder().text(text)
                .metadata(Map.of("file_name", "document.pdf", "chunk_index", chunkIndex))
                .build();
    }
}