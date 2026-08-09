package com.example.rag.graph.infrastructure.neo4j;

import java.util.Collection;
import java.util.List;

import com.example.rag.graph.application.model.GraphPathResult;
import com.example.rag.graph.application.model.GraphQuestion;
import lombok.RequiredArgsConstructor;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class Neo4jGraphRepository {

    private static final String DOCUMENTS_BY_TOPICS = """
            MATCH path =
                (first:Topic)<-[:HAS_TOPIC]-
                (document:Document)-[:HAS_TOPIC]->
                (second:Topic)
            WHERE first.name = $firstEntity
              AND second.name = $secondEntity
            RETURN DISTINCT
                first.name AS firstEntity,
                document.documentId AS documentId,
                second.name AS secondEntity,
                [relationship IN relationships(path) |
                    type(relationship)
                ] AS relationships
            ORDER BY documentId
            """;

    private final Neo4jClient neo4jClient;

    public GraphPathResult search(
            GraphQuestion question
    ) {
        // 탐색 유형별 고정 Cypher 선택
        return switch (question.queryType()) {
            case "DOCUMENTS_BY_TOPICS" -> findDocumentsByTopics(question.entityNames());

            default -> throw new IllegalArgumentException("지원하지 않는 Graph 탐색 유형: " + question.queryType());
        };
    }

    private GraphPathResult findDocumentsByTopics(
            List<String> entityNames
    ) {
        // 탐색 엔티티 수 검증
        if (entityNames.size() != 2) {
            throw new IllegalArgumentException("DOCUMENTS_BY_TOPICS는 엔티티 2개가 필요합니다.");
        }

        String firstEntity = entityNames.get(0);
        String secondEntity = entityNames.get(1);

        // Graph 경로 조회
        Collection<PathRow> rows = neo4jClient
                .query(DOCUMENTS_BY_TOPICS)
                .bind(firstEntity)
                .to("firstEntity")
                .bind(secondEntity)
                .to("secondEntity")
                .fetchAs(PathRow.class)
                .mappedBy((typeSystem, record) ->
                        new PathRow(
                                record.get("firstEntity").asString(),
                                record.get("secondEntity").asString(),
                                record.get("documentId").asString(),
                                record.get("relationships")
                                        .asList(value -> value.asString())
                        )
                )
                .all();

        // 검색 결과 없음 처리
        if (rows.isEmpty()) {
            return new GraphPathResult(
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        PathRow firstRow = rows.iterator().next();

        // 문서 ID 중복 제거
        List<String> documentIds = rows.stream()
                .map(PathRow::documentId)
                .distinct()
                .toList();

        return new GraphPathResult(
                List.of(
                        firstRow.firstEntity(),
                        firstRow.secondEntity()
                ),
                firstRow.relationships(),
                documentIds
        );
    }

    private record PathRow(
            String firstEntity,
            String secondEntity,
            String documentId,
            List<String> relationships
    ) {
    }
}