package com.example.rag.retrieval.application;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import com.example.rag.retrieval.infrastructure.postgres.KeywordSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HybridSearchService {

    private final KeywordSearchService keywordSearchService;
    private final VectorSearchService vectorSearchService;
    private final ReciprocalRankFusion reciprocalRankFusion;

    public List<ReciprocalRankFusion.FusedResult> search(
            String question,
            int keywordTopK,
            int vectorTopK,
            double similarityThreshold,
            int rrfK,
            int finalTopK
    ) {
        // 키워드 검색 실행
        List<KeywordSearchRepository.KeywordSearchRow> keywordResults =
                keywordSearchService.search(
                        question,
                        keywordTopK
                );

        // 벡터 검색 실행
        List<Document> vectorResults =
                vectorSearchService.search(
                        question,
                        vectorTopK,
                        similarityThreshold
                );

        // 검색 결과 공통 순위 모델 변환
        List<ReciprocalRankFusion.RankedResult> keywordRanks = convertKeywordResults(keywordResults);
        List<ReciprocalRankFusion.RankedResult> vectorRanks = convertVectorResults(vectorResults);

        // RRF 순위 통합
        return reciprocalRankFusion.fuse(
                keywordRanks,
                vectorRanks,
                rrfK,
                finalTopK
        );
    }

    private List<ReciprocalRankFusion.RankedResult>
    convertKeywordResults(
            List<KeywordSearchRepository.KeywordSearchRow> results
    ) {
        // 키워드 검색 결과 변환
        return IntStream.range(0, results.size())
                .mapToObj(index -> {
                    KeywordSearchRepository.KeywordSearchRow result = results.get(index);

                    return new ReciprocalRankFusion.RankedResult(
                            createChunkId(
                                    result.fileName(),
                                    result.chunkIndex()
                            ),
                            index + 1
                    );
                })
                .toList();
    }

    private List<ReciprocalRankFusion.RankedResult>
    convertVectorResults(
            List<Document> results
    ) {
        // 벡터 검색 결과 변환
        return IntStream.range(0, results.size())
                .mapToObj(index -> {
                    Document document = results.get(index);

                    return new ReciprocalRankFusion.RankedResult(
                            createChunkId(
                                    document.getMetadata().get("file_name"),
                                    document.getMetadata().get("chunk_index")
                            ),
                            index + 1
                    );
                })
                .toList();
    }

    private String createChunkId(
            Object fileName,
            Object chunkIndex
    ) {
        // 청크 식별 메타데이터 변환
        String fileNameValue = Objects.toString(fileName, "");
        String chunkIndexValue = Objects.toString(chunkIndex, "");

        // 청크 식별 메타데이터 검증
        if (fileNameValue.isBlank() || chunkIndexValue.isBlank()) {
            throw new IllegalStateException("청크 식별 메타데이터가 없습니다.");
        }

        // 청크 ID 생성
        return fileNameValue + "#" + chunkIndexValue;
    }
}