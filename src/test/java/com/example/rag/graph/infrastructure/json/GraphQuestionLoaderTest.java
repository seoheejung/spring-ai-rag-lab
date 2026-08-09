package com.example.rag.graph.infrastructure.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.example.rag.evaluation.infrastructure.json.EvaluationQuestionLoader;
import com.example.rag.graph.application.model.GraphQuestion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

class GraphQuestionLoaderTest {

    @TempDir
    Path tempDir;

    private GraphQuestionLoader graphQuestionLoader;

    @BeforeEach
    void setUp() {
        JsonMapper jsonMapper = JsonMapper.builder().build();

        EvaluationQuestionLoader evaluationQuestionLoader = new EvaluationQuestionLoader(jsonMapper);

        graphQuestionLoader = new GraphQuestionLoader(jsonMapper, evaluationQuestionLoader);
    }

    @Test
    void q001과_q002를_읽는다() throws IOException {
        Path evaluationPath = writeEvaluationQuestions(validEvaluationQuestions());

        Path graphPath = writeGraphQuestions(
                        """
                        [
                          {
                            "questionId": "q-001",
                            "queryType": "DOCUMENTS_BY_TOPICS",
                            "entityNames": [
                              "지식 노드",
                              "실행 노드"
                            ]
                          },
                          {
                            "questionId": "q-002",
                            "queryType": "DOCUMENTS_BY_TOPICS",
                            "entityNames": [
                              "지식 노드",
                              "실행 노드"
                            ]
                          }
                        ]
                        """
                );

        List<GraphQuestion> questions = graphQuestionLoader.load(graphPath, evaluationPath);

        assertThat(questions)
                .extracting(GraphQuestion::questionId)
                .containsExactly("q-001", "q-002");

        assertThat(questions)
                .extracting(GraphQuestion::queryType)
                .containsOnly("DOCUMENTS_BY_TOPICS");

        assertThat(questions.get(0).entityNames()).containsExactly("지식 노드", "실행 노드");
    }

    @Test
    void q003_포함을_차단한다() throws IOException {
        Path evaluationPath = writeEvaluationQuestions(validEvaluationQuestions());

        Path graphPath = writeGraphQuestions(
                        """
                        [
                          {
                            "questionId": "q-001",
                            "queryType": "DOCUMENTS_BY_TOPICS",
                            "entityNames": [
                              "지식 노드",
                              "실행 노드"
                            ]
                          },
                          {
                            "questionId": "q-002",
                            "queryType": "DOCUMENTS_BY_TOPICS",
                            "entityNames": [
                              "지식 노드",
                              "실행 노드"
                            ]
                          },
                          {
                            "questionId": "q-003",
                            "queryType": "DOCUMENTS_BY_TOPICS",
                            "entityNames": [
                              "지식 노드",
                              "실행 노드"
                            ]
                          }
                        ]
                        """
                );

        assertThatThrownBy(() -> graphQuestionLoader.load(graphPath, evaluationPath)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 중복_질문_ID를_차단한다() throws IOException {
        Path evaluationPath = writeEvaluationQuestions(validEvaluationQuestions());

        Path graphPath = writeGraphQuestions(
                        """
                        [
                          {
                            "questionId": "q-001",
                            "queryType": "DOCUMENTS_BY_TOPICS",
                            "entityNames": [
                              "지식 노드",
                              "실행 노드"
                            ]
                          },
                          {
                            "questionId": "q-001",
                            "queryType": "DOCUMENTS_BY_TOPICS",
                            "entityNames": [
                              "지식 노드",
                              "실행 노드"
                            ]
                          },
                          {
                            "questionId": "q-002",
                            "queryType": "DOCUMENTS_BY_TOPICS",
                            "entityNames": [
                              "지식 노드",
                              "실행 노드"
                            ]
                          }
                        ]
                        """
                );

        assertThatThrownBy(() -> graphQuestionLoader.load(graphPath, evaluationPath)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 존재하지_않는_평가_질문_ID를_차단한다() throws IOException {
        Path evaluationPath = writeEvaluationQuestions(
                        """
                        [
                          {
                            "id": "q-001",
                            "question": "질문 1",
                            "type": "exact-term",
                            "expectedDocumentIds": [
                              "graph-engineering-v2026.08.02.pdf"
                            ],
                            "expectedChunkIds": [
                              "graph-engineering-v2026.08.02.pdf#461"
                            ],
                            "answerable": true
                          },
                          {
                            "id": "q-003",
                            "question": "질문 3",
                            "type": "unanswerable",
                            "expectedDocumentIds": [],
                            "expectedChunkIds": [],
                            "answerable": false
                          }
                        ]
                        """
                );

        Path graphPath = writeGraphQuestions(validGraphQuestions());

        assertThatThrownBy(() -> graphQuestionLoader.load(graphPath, evaluationPath)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 빈_엔티티_목록을_차단한다() throws IOException {
        Path evaluationPath = writeEvaluationQuestions(validEvaluationQuestions());

        Path graphPath = writeGraphQuestions(
                        """
                        [
                          {
                            "questionId": "q-001",
                            "queryType": "DOCUMENTS_BY_TOPICS",
                            "entityNames": []
                          },
                          {
                            "questionId": "q-002",
                            "queryType": "DOCUMENTS_BY_TOPICS",
                            "entityNames": [
                              "지식 노드",
                              "실행 노드"
                            ]
                          }
                        ]
                        """
                );

        assertThatThrownBy(() -> graphQuestionLoader.load(graphPath, evaluationPath)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 지원하지_않는_탐색_유형을_차단한다() throws IOException {
        Path evaluationPath = writeEvaluationQuestions(validEvaluationQuestions());

        Path graphPath = writeGraphQuestions(
                        """
                        [
                          {
                            "questionId": "q-001",
                            "queryType": "UNSUPPORTED",
                            "entityNames": [
                              "지식 노드",
                              "실행 노드"
                            ]
                          },
                          {
                            "questionId": "q-002",
                            "queryType": "DOCUMENTS_BY_TOPICS",
                            "entityNames": [
                              "지식 노드",
                              "실행 노드"
                            ]
                          }
                        ]
                        """
                );

        assertThatThrownBy(() -> graphQuestionLoader.load(graphPath, evaluationPath)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 탐색_유형별_엔티티_수를_검증한다() throws IOException {
        Path evaluationPath = writeEvaluationQuestions(validEvaluationQuestions());

        Path graphPath = writeGraphQuestions(
                        """
                        [
                          {
                            "questionId": "q-001",
                            "queryType": "DOCUMENTS_BY_TOPICS",
                            "entityNames": [
                              "지식 노드"
                            ]
                          },
                          {
                            "questionId": "q-002",
                            "queryType": "DOCUMENTS_BY_TOPICS",
                            "entityNames": [
                              "지식 노드",
                              "실행 노드"
                            ]
                          }
                        ]
                        """
                );

        assertThatThrownBy(() -> graphQuestionLoader.load(graphPath, evaluationPath)).isInstanceOf(IllegalStateException.class);
    }

    private Path writeEvaluationQuestions(String json) throws IOException {
        Path path = tempDir.resolve("questions.json");

        Files.writeString(path, json, StandardCharsets.UTF_8);

        return path;
    }

    private Path writeGraphQuestions(String json) throws IOException {
        Path path = tempDir.resolve("graph-questions.json");

        Files.writeString(path, json, StandardCharsets.UTF_8);

        return path;
    }

    private String validGraphQuestions() {
        return """
                [
                  {
                    "questionId": "q-001",
                    "queryType": "DOCUMENTS_BY_TOPICS",
                    "entityNames": [
                      "지식 노드",
                      "실행 노드"
                    ]
                  },
                  {
                    "questionId": "q-002",
                    "queryType": "DOCUMENTS_BY_TOPICS",
                    "entityNames": [
                      "지식 노드",
                      "실행 노드"
                    ]
                  }
                ]
                """;
    }

    private String validEvaluationQuestions() {
        return """
                [
                  {
                    "id": "q-001",
                    "question": "지식과 실행을 한 그래프에 합친다는 것은 무엇인가?",
                    "type": "exact-term",
                    "expectedDocumentIds": [
                      "graph-engineering-v2026.08.02.pdf"
                    ],
                    "expectedChunkIds": [
                      "graph-engineering-v2026.08.02.pdf#461"
                    ],
                    "answerable": true
                  },
                  {
                    "id": "q-002",
                    "question": "에이전트 실행이 어떤 사실을 읽었는지 추적하려면 그래프를 어떻게 구성해야 하는가?",
                    "type": "semantic-paraphrase",
                    "expectedDocumentIds": [
                      "graph-engineering-v2026.08.02.pdf"
                    ],
                    "expectedChunkIds": [
                      "graph-engineering-v2026.08.02.pdf#461"
                    ],
                    "answerable": true
                  },
                  {
                    "id": "q-003",
                    "question": "Spring AI가 제공하는 양자 암호화 알고리즘은 무엇인가?",
                    "type": "unanswerable",
                    "expectedDocumentIds": [],
                    "expectedChunkIds": [],
                    "answerable": false
                  }
                ]
                """;
    }
}