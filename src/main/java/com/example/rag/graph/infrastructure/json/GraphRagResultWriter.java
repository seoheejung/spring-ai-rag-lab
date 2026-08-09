package com.example.rag.graph.infrastructure.json;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.example.rag.graph.application.model.GraphRagComparison;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class GraphRagResultWriter {

    private final JsonMapper jsonMapper;

    public void write(Path path, GraphRagComparison result) {
        try {
            // 결과 저장 디렉터리 생성
            Path parent = path.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            // Graph RAG 비교 결과 저장
            jsonMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), result);
        }
        catch (IOException exception) {
            throw new IllegalStateException("Graph RAG 결과 저장에 실패했습니다: " + path, exception);
        }
    }
}