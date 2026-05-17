package com.javaee.aiservice.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * 重排序器
 * 支持多种重排序策略：BM25融合、Cross-Encoder、自定义规则等
 */
@Component
public class Reranker {

    private static final Logger log = LoggerFactory.getLogger(Reranker.class);
    private static final int MAX_DOCUMENT_CHARS = 4096;

    @Value("${spring.ai.dashscope.rerank.api-key:${spring.ai.dashscope.api-key}}")
    private String apiKey;

    @Value("${spring.ai.dashscope.rerank.model:gte-rerank-v2}")
    private String rerankModel;

    @Value("${spring.ai.dashscope.rerank.endpoint:https://dashscope.aliyuncs.com/api/v1/services/rerank/text-rerank/text-rerank}")
    private String rerankEndpoint;

    @Value("${spring.ai.dashscope.rerank.enabled:true}")
    private boolean rerankEnabled;

    @Value("${spring.ai.dashscope.rerank.timeout-ms:10000}")
    private int rerankTimeoutMs;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * 重排序策略枚举
     */
    public enum RerankStrategy {
        BM25_FUSION,      // BM25与向量相似度融合
        CROSS_ENCODER,    // Cross-Encoder重排序
        HYBRID            // 混合策略
    }

    /**
     * 重排序结果
     */
    public static class RerankResult {
        private String documentId;
        private int originalIndex;
        private float originalScore;
        private float rerankScore;
        private float finalScore;

        public RerankResult(String documentId, int originalIndex, float originalScore, float rerankScore, float finalScore) {
            this.documentId = documentId;
            this.originalIndex = originalIndex;
            this.originalScore = originalScore;
            this.rerankScore = rerankScore;
            this.finalScore = finalScore;
        }

        public String getDocumentId() { return documentId; }
        public int getOriginalIndex() { return originalIndex; }
        public float getOriginalScore() { return originalScore; }
        public float getRerankScore() { return rerankScore; }
        public float getFinalScore() { return finalScore; }
    }

    private static class CrossEncoderScores {
        private final List<Float> scores;
        private final String provider;

        private CrossEncoderScores(List<Float> scores, String provider) {
            this.scores = scores;
            this.provider = provider;
        }
    }

    /**
     * 对检索结果进行重排序
     * @param query 查询词
     * @param candidates 候选结果列表（包含id, similarity, content）
     * @param strategy 重排序策略
     * @param topK 返回数量
     * @return 重排序后的结果
     */
    public List<Map<String, Object>> rerank(String query, List<Map<String, Object>> candidates, 
                                            RerankStrategy strategy, int topK) {
        if (candidates == null || candidates.isEmpty() || topK <= 0) {
            return Collections.emptyList();
        }

        RerankStrategy effectiveStrategy = strategy == null ? RerankStrategy.HYBRID : strategy;
        log.info("开始重排序: strategy={}, candidates={}", effectiveStrategy, candidates.size());

        List<RerankResult> rerankResults = new ArrayList<>();
        CrossEncoderScores crossEncoderScores = needsCrossEncoder(effectiveStrategy)
                ? computeCrossEncoderScores(query, candidates)
                : new CrossEncoderScores(Collections.emptyList(), "none");

        for (int i = 0; i < candidates.size(); i++) {
            Map<String, Object> candidate = candidates.get(i);
            String docId = asString(candidate.get("id"), String.valueOf(i));
            float originalScore = asFloat(candidate.get("similarity"), 0.0f);
            String content = asString(candidate.get("content"), "");

            float rerankScore = 0.0f;
            float finalScore = originalScore;

            switch (effectiveStrategy) {
                case BM25_FUSION:
                    rerankScore = computeBM25(query, content);
                    finalScore = fuseScores(originalScore, rerankScore, 0.6f, 0.4f);
                    break;
                case CROSS_ENCODER:
                    rerankScore = crossEncoderScores.scores.get(i);
                    finalScore = rerankScore;
                    break;
                case HYBRID:
                    float bm25Score = computeBM25(query, content);
                    float ceScore = crossEncoderScores.scores.get(i);
                    rerankScore = (bm25Score + ceScore) / 2;
                    finalScore = fuseScores(originalScore, rerankScore, 0.5f, 0.5f);
                    break;
            }

            rerankResults.add(new RerankResult(docId, i, originalScore, rerankScore, finalScore));
        }

        // 按最终分数排序
        rerankResults.sort((a, b) -> Float.compare(b.getFinalScore(), a.getFinalScore()));

        // 构建返回结果
        List<Map<String, Object>> results = new ArrayList<>();
        int limit = Math.min(topK, rerankResults.size());
        
        for (int i = 0; i < limit; i++) {
            RerankResult result = rerankResults.get(i);
            Map<String, Object> candidate = candidates.get(result.getOriginalIndex());
            
            Map<String, Object> finalResult = new HashMap<>(candidate);
            finalResult.put("originalScore", result.getOriginalScore());
            finalResult.put("rerankScore", result.getRerankScore());
            finalResult.put("finalScore", result.getFinalScore());
            finalResult.put("rerankProvider", providerFor(effectiveStrategy, crossEncoderScores.provider));
            results.add(finalResult);
        }

        log.info("重排序完成，返回{}条结果", results.size());
        return results;
    }

    /**
     * 计算BM25分数（简化实现）
     */
    private float computeBM25(String query, String document) {
        if (query == null || document == null) {
            return 0.0f;
        }

        String[] queryTerms = tokenize(query.toLowerCase());
        String[] docTerms = tokenize(document.toLowerCase());

        int docLength = docTerms.length;
        if (docLength == 0 || queryTerms.length == 0) {
            return 0.0f;
        }

        float score = 0.0f;
        int totalDocs = 1000; // 假设总文档数
        
        for (String term : queryTerms) {
            if (term.isEmpty()) continue;
            
            int termFreq = 0;
            for (String docTerm : docTerms) {
                if (docTerm.contains(term) || term.contains(docTerm)) {
                    termFreq++;
                }
            }

            if (termFreq > 0) {
                float idf = (float) Math.log((totalDocs + 0.5) / (1 + 0.5));
                float tf = (float) termFreq / docLength;
                float bm25 = (float)(idf * tf * (2.2 + 1) / (tf + 2.2 * (1 - 0.75 + 0.75 * docLength / 200)));
                score += bm25;
            }
        }

        // 归一化到[0,1]
        return Math.min(1.0f, score / queryTerms.length);
    }

    private CrossEncoderScores computeCrossEncoderScores(String query, List<Map<String, Object>> candidates) {
        if (!rerankEnabled || isBlank(apiKey) || isBlank(query)) {
            return new CrossEncoderScores(computeLocalCrossEncoderScores(query, candidates), "local-fallback");
        }

        try {
            List<String> documents = new ArrayList<>();
            for (Map<String, Object> candidate : candidates) {
                documents.add(truncate(asString(candidate.get("content"), "")));
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", rerankModel);
            payload.put("input", Map.of(
                    "query", query,
                    "documents", documents
            ));
            payload.put("parameters", Map.of(
                    "return_documents", false,
                    "top_n", candidates.size()
            ));

            HttpRequest request = HttpRequest.newBuilder(URI.create(rerankEndpoint))
                    .timeout(Duration.ofMillis(Math.max(1000, rerankTimeoutMs)))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("DashScope rerank HTTP " + response.statusCode());
            }

            return new CrossEncoderScores(parseDashScopeScores(response.body(), candidates.size()), rerankModel);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("真实重排请求被中断，降级到本地重排: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("真实重排调用失败，降级到本地重排: {}", e.getMessage());
        }
        return new CrossEncoderScores(computeLocalCrossEncoderScores(query, candidates), "local-fallback");
    }

    private List<Float> parseDashScopeScores(String body, int expectedSize) throws Exception {
        JsonNode results = objectMapper.readTree(body).path("output").path("results");
        if (!results.isArray()) {
            throw new IllegalStateException("DashScope rerank 返回缺少 output.results");
        }

        List<Float> scores = new ArrayList<>(Collections.nCopies(expectedSize, 0.0f));
        int parsed = 0;
        for (JsonNode result : results) {
            int index = result.path("index").asInt(-1);
            if (index < 0 || index >= expectedSize) {
                continue;
            }
            JsonNode scoreNode = result.has("relevance_score")
                    ? result.path("relevance_score")
                    : result.path("score");
            if (!scoreNode.isNumber()) {
                continue;
            }
            scores.set(index, clamp((float) scoreNode.asDouble()));
            parsed++;
        }

        if (parsed == 0) {
            throw new IllegalStateException("DashScope rerank 返回没有有效分数");
        }
        return scores;
    }

    /**
     * 本地兜底分数只用于外部 rerank 模型不可用时，避免检索链路失败。
     */
    private List<Float> computeLocalCrossEncoderScores(String query, List<Map<String, Object>> candidates) {
        List<Float> scores = new ArrayList<>();
        for (Map<String, Object> candidate : candidates) {
            scores.add(computeLocalCrossEncoder(query, asString(candidate.get("content"), "")));
        }
        return scores;
    }

    private float computeLocalCrossEncoder(String query, String document) {
        if (query == null || document == null) {
            return 0.0f;
        }

        String queryLower = query.toLowerCase();
        String docLower = document.toLowerCase();

        int matchCount = 0;
        String[] queryTerms = tokenize(queryLower);
        if (queryTerms.length == 0) {
            return 0.0f;
        }
        
        for (String term : queryTerms) {
            if (docLower.contains(term)) {
                matchCount++;
            }
        }

        // 计算位置权重（关键词出现在文档开头权重更高）
        float positionBonus = 0.0f;
        for (String term : queryTerms) {
            int idx = docLower.indexOf(term);
            if (idx >= 0 && idx < docLower.length() / 3) {
                positionBonus += 0.1f;
            }
        }

        float baseScore = (float) matchCount / queryTerms.length;
        return clamp(baseScore + positionBonus);
    }

    /**
     * 融合两个分数
     * @param score1 分数1
     * @param score2 分数2
     * @param weight1 分数1权重
     * @param weight2 分数2权重
     * @return 融合后的分数
     */
    private float fuseScores(float score1, float score2, float weight1, float weight2) {
        return score1 * weight1 + score2 * weight2;
    }

    private boolean needsCrossEncoder(RerankStrategy strategy) {
        return strategy == RerankStrategy.CROSS_ENCODER || strategy == RerankStrategy.HYBRID;
    }

    private String providerFor(RerankStrategy strategy, String crossEncoderProvider) {
        if (strategy == RerankStrategy.BM25_FUSION) {
            return "bm25";
        }
        if (strategy == RerankStrategy.HYBRID) {
            return "hybrid:" + crossEncoderProvider;
        }
        return crossEncoderProvider;
    }

    private float asFloat(Object value, float defaultValue) {
        return value instanceof Number ? ((Number) value).floatValue() : defaultValue;
    }

    private String asString(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String truncate(String value) {
        if (value == null || value.length() <= MAX_DOCUMENT_CHARS) {
            return value == null ? "" : value;
        }
        return value.substring(0, MAX_DOCUMENT_CHARS);
    }

    private float clamp(float score) {
        return Math.max(0.0f, Math.min(1.0f, score));
    }

    private String[] tokenize(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            return new String[0];
        }
        if (normalized.contains(" ")) {
            return normalized.split("\\s+");
        }
        return normalized.codePoints()
                .mapToObj(cp -> new String(Character.toChars(cp)))
                .toArray(String[]::new);
    }

    /**
     * 获取所有支持的重排序策略
     */
    public List<String> getSupportedStrategies() {
        List<String> strategies = new ArrayList<>();
        for (RerankStrategy strategy : RerankStrategy.values()) {
            strategies.add(strategy.name());
        }
        return strategies;
    }
}
