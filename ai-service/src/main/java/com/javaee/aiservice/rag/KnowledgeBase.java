package com.javaee.aiservice.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class KnowledgeBase {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBase.class);
    private static final String DOCUMENT_PREFIX = "doc:";
    private static final String CONTENT_PREFIX = "content:";
    private static final String SEGMENT_PREFIX = "segment:";
    private static final String DOC_SEGMENTS_PREFIX = "doc_segments:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private DocumentVectorizer vectorizer;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private Reranker reranker;

    @Autowired
    private DocumentSegmenter documentSegmenter;

    public void addDocument(String documentId, String content, Map<String, Object> metadata) {
        addDocumentWithSegment(documentId, content, metadata, DocumentSegmenter.StrategyType.AUTO);
    }

    public void addDocumentWithSegment(String documentId, String content, Map<String, Object> metadata,
                                       DocumentSegmenter.StrategyType strategyType) {
        log.info("添加文档到知识库: documentId={}, strategy={}", documentId, strategyType);

        try {
            String docKey = DOCUMENT_PREFIX + documentId;
            String contentKey = CONTENT_PREFIX + documentId;

            Map<String, Object> docMetadata = normalizeMetadata(metadata);
            docMetadata.put("strategy", strategyType.name());
            docMetadata.put("totalLength", content.length());

            redisTemplate.opsForValue().set(contentKey, content);
            redisTemplate.opsForHash().putAll(docKey, docMetadata);

            List<SegmentStrategy.Segment> segments = documentSegmenter.segment(documentId, content, strategyType);

            if (segments.isEmpty()) {
                log.warn("文档分段结果为空，直接存储完整文档");
                float[] vector = vectorizer.vectorize(content);
                vectorStore.store(documentId, vector, docMetadata);
                return;
            }

            List<String> segmentIds = new ArrayList<>();
            for (SegmentStrategy.Segment segment : segments) {
                String segmentId = segment.getSegmentId();
                segmentIds.add(segmentId);

                String segmentContentKey = SEGMENT_PREFIX + segmentId;
                redisTemplate.opsForValue().set(segmentContentKey, segment.getContent());

                Map<String, Object> segmentMetadata = new HashMap<>(docMetadata);
                segmentMetadata.put("documentId", documentId);
                segmentMetadata.put("segmentIndex", segment.getIndex());
                segmentMetadata.put("segmentTitle", segment.getTitle());
                segmentMetadata.put("charCount", segment.getCharCount());

                float[] vector = vectorizer.vectorize(segment.getContent());
                vectorStore.store(segmentId, vector, segmentMetadata);
            }

            redisTemplate.opsForValue().set(DOC_SEGMENTS_PREFIX + documentId, segmentIds);

            log.info("文档添加成功: documentId={}, 分段数={}", documentId, segments.size());
        } catch (Exception e) {
            log.error("添加文档失败", e);
            throw new RuntimeException("添加文档失败: " + e.getMessage(), e);
        }
    }

    public void addDocument(String documentId, String content, Map<String, Object> metadata,
                           DocumentSegmenter.StrategyType strategyType) {
        addDocumentWithSegment(documentId, content, metadata, strategyType);
    }

    public void removeDocument(String documentId) {
        log.info("从知识库移除文档: documentId={}", documentId);

        try {
            List<String> segmentIds = getSegmentIds(documentId);

            for (String segmentId : segmentIds) {
                redisTemplate.delete(SEGMENT_PREFIX + segmentId);
                vectorStore.delete(segmentId);
            }

            redisTemplate.delete(DOC_SEGMENTS_PREFIX + documentId);
            redisTemplate.delete(DOCUMENT_PREFIX + documentId);
            redisTemplate.delete(CONTENT_PREFIX + documentId);

            log.info("文档移除成功: documentId={}, 删除了{}个分段", documentId, segmentIds.size());
        } catch (Exception e) {
            log.error("移除文档失败", e);
            throw new RuntimeException("移除文档失败: " + e.getMessage(), e);
        }
    }

    public String getDocumentContent(String documentId) {
        try {
            return (String) redisTemplate.opsForValue().get(CONTENT_PREFIX + documentId);
        } catch (Exception e) {
            log.warn("获取文档内容失败", e);
            return null;
        }
    }

    public String getSegmentContent(String segmentId) {
        try {
            return (String) redisTemplate.opsForValue().get(SEGMENT_PREFIX + segmentId);
        } catch (Exception e) {
            log.warn("获取分段内容失败", e);
            return null;
        }
    }

    public List<String> getSegmentIds(String documentId) {
        try {
            Object segmentIdsObj = redisTemplate.opsForValue().get(DOC_SEGMENTS_PREFIX + documentId);
            if (segmentIdsObj == null) {
                return Collections.emptyList();
            }
            if (segmentIdsObj instanceof List) {
                return ((List<?>) segmentIdsObj).stream()
                        .map(Object::toString)
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("获取文档分段ID列表失败", e);
            return Collections.emptyList();
        }
    }

    public Map<String, Object> getDocumentMetadata(String documentId) {
        try {
            Map<Object, Object> hash = redisTemplate.opsForHash().entries(DOCUMENT_PREFIX + documentId);
            Map<String, Object> metadata = new HashMap<>();
            for (Map.Entry<Object, Object> entry : hash.entrySet()) {
                metadata.put(entry.getKey().toString(), entry.getValue());
            }
            return metadata;
        } catch (Exception e) {
            log.warn("获取文档元数据失败", e);
            return Collections.emptyMap();
        }
    }

    public List<Map<String, Object>> getDocumentSegments(String documentId) {
        log.info("获取文档分段: documentId={}", documentId);

        try {
            List<String> segmentIds = getSegmentIds(documentId);
            List<Map<String, Object>> segments = new ArrayList<>();

            for (String segmentId : segmentIds) {
                Map<String, Object> segmentInfo = new HashMap<>();
                segmentInfo.put("segmentId", segmentId);
                segmentInfo.put("content", getSegmentContent(segmentId));
                segments.add(segmentInfo);
            }

            return segments;
        } catch (Exception e) {
            log.error("获取文档分段失败", e);
            throw new RuntimeException("获取文档分段失败: " + e.getMessage(), e);
        }
    }

    public List<Map<String, Object>> search(String query, int topK) {
        return search(query, topK, DocumentSegmenter.StrategyType.CHAPTER);
    }

    public List<Map<String, Object>> search(String query, int topK,
                                            DocumentSegmenter.StrategyType strategyType) {
        return search(query, topK, strategyType, Collections.emptyMap());
    }

    public List<Map<String, Object>> search(String query, int topK,
                                            DocumentSegmenter.StrategyType strategyType,
                                            Map<String, Object> filters) {
        log.info("搜索知识库: query={}, topK={}, strategy={}", query, topK, strategyType);

        try {
            float[] queryVector = vectorizer.vectorize(query);
            List<Map<String, Object>> results = vectorStore.search(queryVector, topK, filters);

            for (Map<String, Object> result : results) {
                String id = (String) result.get("id");
                String content = getSegmentContent(id);
                if (content == null) {
                    content = getDocumentContent(id);
                }
                result.put("content", content);
            }

            return results;
        } catch (Exception e) {
            log.error("知识库搜索失败", e);
            throw new RuntimeException("知识库搜索失败: " + e.getMessage(), e);
        }
    }

    public List<Map<String, Object>> hybridSearch(String query, int topK) {
        return hybridSearch(query, topK, DocumentSegmenter.StrategyType.CHAPTER);
    }

    public List<Map<String, Object>> hybridSearch(String query, int topK,
                                                   DocumentSegmenter.StrategyType strategyType) {
        return hybridSearch(query, topK, strategyType, Collections.emptyMap());
    }

    public List<Map<String, Object>> hybridSearch(String query, int topK,
                                                   DocumentSegmenter.StrategyType strategyType,
                                                   Map<String, Object> filters) {
        log.info("混合检索: query={}, topK={}, strategy={}", query, topK, strategyType);

        try {
            float[] queryVector = vectorizer.vectorize(query);
            List<Map<String, Object>> vectorResults = vectorStore.search(queryVector, topK * 3, filters);

            List<Map<String, Object>> bm25Results = bm25Search(query, topK * 3, filters);

            Set<String> seenIds = new HashSet<>();
            List<Map<String, Object>> combinedResults = new ArrayList<>();

            for (Map<String, Object> result : vectorResults) {
                String id = (String) result.get("id");
                if (!seenIds.contains(id)) {
                    seenIds.add(id);
                    String content = getSegmentContent(id);
                    if (content == null) {
                        content = getDocumentContent(id);
                    }
                    result.put("content", content);
                    result.put("source", "vector");
                    combinedResults.add(result);
                }
            }

            for (Map<String, Object> result : bm25Results) {
                String id = (String) result.get("id");
                if (!seenIds.contains(id)) {
                    seenIds.add(id);
                    String content = getSegmentContent(id);
                    if (content == null) {
                        content = getDocumentContent(id);
                    }
                    result.put("content", content);
                    result.put("source", "bm25");
                    combinedResults.add(result);
                }
            }

            return combinedResults.subList(0, Math.min(topK, combinedResults.size()));

        } catch (Exception e) {
            log.error("混合检索失败", e);
            throw new RuntimeException("混合检索失败: " + e.getMessage(), e);
        }
    }

    public List<Map<String, Object>> hybridSearchWithRerank(String query, int topK,
                                                            Reranker.RerankStrategy rerankStrategy,
                                                            DocumentSegmenter.StrategyType strategyType) {
        return hybridSearchWithRerank(query, topK, rerankStrategy, strategyType, Collections.emptyMap());
    }

    public List<Map<String, Object>> hybridSearchWithRerank(String query, int topK,
                                                            Reranker.RerankStrategy rerankStrategy,
                                                            DocumentSegmenter.StrategyType strategyType,
                                                            Map<String, Object> filters) {
        log.info("混合检索加重排序: query={}, topK={}, strategy={}, rerankStrategy={}",
                query, topK, strategyType, rerankStrategy);

        try {
            List<Map<String, Object>> candidates = hybridSearch(query, topK * 3, strategyType, filters);

            List<Map<String, Object>> results = reranker.rerank(query, candidates, rerankStrategy, topK);

            return results;
        } catch (Exception e) {
            log.error("混合检索加重排序失败", e);
            throw new RuntimeException("混合检索加重排序失败: " + e.getMessage(), e);
        }
    }

    public List<Map<String, Object>> hybridSearchWithRerank(String query, int topK,
                                                            Reranker.RerankStrategy rerankStrategy) {
        return hybridSearchWithRerank(query, topK, rerankStrategy, DocumentSegmenter.StrategyType.CHAPTER);
    }

    public List<Map<String, Object>> hybridSearchWithRerank(String query, int topK,
                                                            Reranker.RerankStrategy rerankStrategy,
                                                            String userId,
                                                            String knowledgeBaseId) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("userId", userId);
        filters.put("knowledgeBaseId", knowledgeBaseId);
        return hybridSearchWithRerank(query, topK, rerankStrategy, DocumentSegmenter.StrategyType.CHAPTER, filters);
    }

    private List<Map<String, Object>> bm25Search(String query, int topK, Map<String, Object> filters) {
        List<Map<String, Object>> results = new ArrayList<>();
        List<String> contentKeys = scanKeys(CONTENT_PREFIX + "*", 1000);
        List<String> segmentKeys = scanKeys(SEGMENT_PREFIX + "*", 1000);

        Set<String> allKeys = new LinkedHashSet<>();
        if (contentKeys != null) allKeys.addAll(contentKeys);
        if (segmentKeys != null) allKeys.addAll(segmentKeys);

        if (allKeys.isEmpty()) {
            return results;
        }

        for (String key : allKeys) {
            String docId = key.substring(key.lastIndexOf(":") + 1);
            Map<String, Object> metadata = key.startsWith(SEGMENT_PREFIX)
                    ? vectorStoreMetadata(docId)
                    : getDocumentMetadata(docId);
            if (!matchesFilters(metadata, filters)) {
                continue;
            }
            String content = (String) redisTemplate.opsForValue().get(key);

            if (content != null) {
                float score = computeBM25(query, content);
                if (score > 0) {
                    results.add(Map.of(
                        "id", docId,
                        "similarity", score
                    ));
                }
            }
        }

        results.sort((a, b) -> Float.compare(
            ((Number) b.get("similarity")).floatValue(),
            ((Number) a.get("similarity")).floatValue()
        ));

        return results.subList(0, Math.min(topK, results.size()));
    }

    private float computeBM25(String query, String document) {
        if (query == null || document == null) {
            return 0.0f;
        }

        String[] queryTerms = query.toLowerCase().split("\\s+");
        String[] docTerms = document.toLowerCase().split("\\s+");

        int docLength = docTerms.length;
        if (docLength == 0) {
            return 0.0f;
        }

        float score = 0.0f;
        for (String term : queryTerms) {
            if (term.isEmpty()) continue;

            int termFreq = 0;
            for (String docTerm : docTerms) {
                if (docTerm.contains(term) || term.contains(docTerm)) {
                    termFreq++;
                }
            }

            if (termFreq > 0) {
                float tf = (float) termFreq / docLength;
                float bm25 = (float)(tf * (2.2 + 1) / (tf + 2.2));
                score += bm25;
            }
        }

        return score / queryTerms.length;
    }

    public List<String> getAllDocumentIds() {
        try {
            List<String> keys = scanKeys(DOCUMENT_PREFIX + "*", 1000);
            if (keys == null) {
                return Collections.emptyList();
            }
            return keys.stream()
                .map(key -> key.substring(DOCUMENT_PREFIX.length()))
                .toList();
        } catch (Exception e) {
            log.warn("获取文档ID列表失败", e);
            return Collections.emptyList();
        }
    }

    public List<String> getAllDocumentIds(String userId, String knowledgeBaseId) {
        return getAllDocumentIds().stream()
                .filter(documentId -> matchesFilters(getDocumentMetadata(documentId), Map.of(
                        "userId", userId,
                        "knowledgeBaseId", knowledgeBaseId
                )))
                .toList();
    }

    public void updateDocument(String documentId, String content, Map<String, Object> metadata) {
        log.info("更新文档: documentId={}", documentId);
        removeDocument(documentId);
        addDocumentWithSegment(documentId, content, metadata, DocumentSegmenter.StrategyType.AUTO);
    }

    public void updateDocument(String documentId, String content, Map<String, Object> metadata,
                              DocumentSegmenter.StrategyType strategyType) {
        log.info("更新文档: documentId={}, strategy={}", documentId, strategyType);
        removeDocument(documentId);
        addDocumentWithSegment(documentId, content, metadata, strategyType);
    }

    public Map<String, Object> getStatistics() {
        return getStatistics(null, null);
    }

    public Map<String, Object> getStatistics(String userId, String knowledgeBaseId) {
        Map<String, Object> stats = new HashMap<>();

        List<String> docIds = (userId == null || knowledgeBaseId == null)
                ? getAllDocumentIds()
                : getAllDocumentIds(userId, knowledgeBaseId);
        stats.put("documentCount", docIds.size());

        int totalSegments = 0;
        for (String docId : docIds) {
            totalSegments += getSegmentIds(docId).size();
        }
        stats.put("segmentCount", totalSegments);

        long totalContentSize = 0;
        for (String docId : docIds) {
            String content = getDocumentContent(docId);
            if (content != null) {
                totalContentSize += content.length();
            }
        }
        stats.put("totalContentSize", totalContentSize);

        return stats;
    }

    private Map<String, Object> normalizeMetadata(Map<String, Object> metadata) {
        Map<String, Object> normalized = metadata == null ? new HashMap<>() : new HashMap<>(metadata);
        normalized.putIfAbsent("userId", "system");
        normalized.putIfAbsent("knowledgeBaseId", "default");
        return normalized;
    }

    private List<String> scanKeys(String pattern, int count) {
        List<String> keys = new ArrayList<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(count).build();
        try (var cursor = redisTemplate.getConnectionFactory().getConnection().scan(options)) {
            while (cursor.hasNext()) {
                keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
            }
        }
        return keys;
    }

    private boolean matchesFilters(Map<String, Object> metadata, Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return true;
        }
        for (Map.Entry<String, Object> filter : filters.entrySet()) {
            Object expected = filter.getValue();
            if (expected == null || expected.toString().isBlank()) {
                continue;
            }
            Object actual = metadata.get(filter.getKey());
            if (actual == null || !expected.toString().equals(actual.toString())) {
                return false;
            }
        }
        return true;
    }

    private Map<String, Object> vectorStoreMetadata(String id) {
        try {
            Map<Object, Object> hash = redisTemplate.opsForHash().entries("metadata:" + id);
            Map<String, Object> metadata = new HashMap<>();
            for (Map.Entry<Object, Object> entry : hash.entrySet()) {
                metadata.put(entry.getKey().toString(), entry.getValue());
            }
            return metadata;
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
