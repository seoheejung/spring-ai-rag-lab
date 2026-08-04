package com.example.rag.evaluation.infrastructure.json;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.example.rag.evaluation.application.model.EvaluationSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class EvaluationResultWriter {

    private final JsonMapper jsonMapper;

    public void write(
            Path path,
            EvaluationSummary summary
    ) {
        try {
            // 결과 저장 디렉터리 생성
            Path parentPath = path.getParent();

            if (parentPath != null) {
                Files.createDirectories(parentPath);
            }

            // 평가 결과 JSON 저장
            jsonMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValue(path.toFile(), summary);
        }
        catch (IOException exception) {
            throw new IllegalStateException("평가 결과 저장에 실패했습니다: " + path, exception);
        }
    }
}