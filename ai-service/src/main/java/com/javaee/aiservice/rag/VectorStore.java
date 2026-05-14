package com.javaee.aiservice.rag;

import com.github.jelmerk.knn.DistanceFunctions;
import com.github.jelmerk.knn.Item;
import com.github.jelmerk.knn.SearchResult;
import com.github.jelmerk.knn.hnsw.HnswIndex;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 向量存储
 * - 写入：Redis 持久化向量与元数据；HNSW in-memory 索引建立近邻图
 * - 检索：先在 HNSW 中做近似最近邻搜索，再用 Redis 中的元数据做过滤
 * - 启动：从 Redis 灌入历史向量重建索引
 */
@Component
public class VectorStore {

    private static final Logger log = LoggerFactory.getLogger(VectorStore.class);
    private static final String VECTOR_PREFIX = "vector:";
    private static final String METADATA_PREFIX = "metadata:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${ai.vector.dimension:1536}")
    private int defaultDimension;

    @Value("${ai.vector.hnsw.m:16}")
    private int hnswM;

    @Value("${ai.vector.hnsw.ef-construction:200}")
    private int hnswEfConstruction;

    @Value("${ai.vector.hnsw.ef:128}")
    private int hnswEf;

    @Value("${ai.vector.hnsw.max-items:200000}")
    private int hnswMaxItems;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private volatile HnswIndex<String, float[], FloatArrayItem, Float> index;
    private volatile int dimension;

    @PostConstruct
    public void initialize() {
        try {
            warmupFromRedis();
        } catch (Exception e) {
            log.warn("HNSW 索引初始化失败，将在首次写入时构建: {}", e.getMessage());
        }
    }

    /** 启动时从 Redis 重建 HNSW 索引。 */
    private void warmupFromRedis() {
        List<String> keys = scanKeys(VECTOR_PREFIX + "*", 1000);
        if (keys == null || keys.isEmpty()) {
            log.info("未发现已有向量数据，HNSW 索引延迟构建");
            return;
        }
        for (String key : keys) {
            String id = key.substring(VECTOR_PREFIX.length());
            float[] vector = convertToFloatArray(redisTemplate.opsForValue().get(key));
            if (vector == null) {
                continue;
            }
            ensureIndex(vector.length);
            try {
                index.add(new FloatArrayItem(id, vector));
            } catch (Exception e) {
                log.warn("HNSW 索引重建时跳过向量 id={}: {}", id, e.getMessage());
            }
        }
        log.info("HNSW 索引重建完成，载入 {} 个向量", index == null ? 0 : index.size());
    }

    /**
     * 存储向量
     */
    public void store(String id, float[] vector, Map<String, Object> metadata) {
        log.info("存储向量: id={}, dimension={}", id, vector.length);
        try {
            String vectorKey = VECTOR_PREFIX + id;
            String metadataKey = METADATA_PREFIX + id;

            List<Float> vectorList = new ArrayList<>(vector.length);
            for (float v : vector) {
                vectorList.add(v);
            }
            redisTemplate.opsForValue().set(vectorKey, vectorList);
            redisTemplate.opsForHash().putAll(metadataKey, metadata);

            ensureIndex(vector.length);
            lock.writeLock().lock();
            try {
                index.remove(id, 0L);
                index.add(new FloatArrayItem(id, vector));
            } finally {
                lock.writeLock().unlock();
            }

            log.info("向量存储成功: id={}", id);
        } catch (Exception e) {
            log.error("向量存储失败", e);
            throw new RuntimeException("向量存储失败: " + e.getMessage(), e);
        }
    }

    public List<Map<String, Object>> search(float[] queryVector, int topK) {
        return search(queryVector, topK, Collections.emptyMap());
    }

    public List<Map<String, Object>> search(float[] queryVector, int topK, Map<String, Object> filters) {
        log.info("搜索相似向量: topK={}", topK);

        if (index == null || index.size() == 0) {
            log.warn("HNSW 索引为空");
            return Collections.emptyList();
        }

        try {
            int candidateCount = Math.max(topK * 4, topK + 16);
            List<SearchResult<FloatArrayItem, Float>> hits;
            lock.readLock().lock();
            try {
                hits = index.findNearest(queryVector, candidateCount);
            } finally {
                lock.readLock().unlock();
            }

            List<Map<String, Object>> finalResults = new ArrayList<>();
            for (SearchResult<FloatArrayItem, Float> hit : hits) {
                if (finalResults.size() >= topK) {
                    break;
                }
                String id = hit.item().id();
                Map<String, Object> metadata = getMetadata(id);
                if (!matchesFilters(metadata, filters)) {
                    continue;
                }
                float similarity = 1.0f - hit.distance();
                Map<String, Object> item = new HashMap<>();
                item.put("id", id);
                item.put("similarity", similarity);
                item.putAll(metadata);
                finalResults.add(item);
            }

            log.info("搜索完成，找到{}个结果", finalResults.size());
            return finalResults;
        } catch (Exception e) {
            log.error("向量搜索失败", e);
            throw new RuntimeException("向量搜索失败: " + e.getMessage(), e);
        }
    }

    public void delete(String id) {
        log.info("删除向量: id={}", id);
        try {
            redisTemplate.delete(VECTOR_PREFIX + id);
            redisTemplate.delete(METADATA_PREFIX + id);
            if (index != null) {
                lock.writeLock().lock();
                try {
                    index.remove(id, 0L);
                } finally {
                    lock.writeLock().unlock();
                }
            }
            log.info("向量删除成功: id={}", id);
        } catch (Exception e) {
            log.error("向量删除失败", e);
            throw new RuntimeException("向量删除失败: " + e.getMessage(), e);
        }
    }

    private void ensureIndex(int vectorDimension) {
        if (index != null) {
            return;
        }
        lock.writeLock().lock();
        try {
            if (index == null) {
                int dim = vectorDimension > 0 ? vectorDimension : defaultDimension;
                this.dimension = dim;
                this.index = HnswIndex
                        .newBuilder(dim, DistanceFunctions.FLOAT_COSINE_DISTANCE, hnswMaxItems)
                        .withM(hnswM)
                        .withEfConstruction(hnswEfConstruction)
                        .withEf(hnswEf)
                        .build();
                log.info("HNSW 索引已创建: dimension={}, M={}, efConstruction={}, ef={}, maxItems={}",
                        dim, hnswM, hnswEfConstruction, hnswEf, hnswMaxItems);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private Map<String, Object> getMetadata(String id) {
        try {
            Map<Object, Object> hash = redisTemplate.opsForHash().entries(METADATA_PREFIX + id);
            Map<String, Object> metadata = new HashMap<>();
            for (Map.Entry<Object, Object> entry : hash.entrySet()) {
                metadata.put(entry.getKey().toString(), entry.getValue());
            }
            return metadata;
        } catch (Exception e) {
            log.warn("获取元数据失败", e);
            return Collections.emptyMap();
        }
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

    private float[] convertToFloatArray(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof float[]) {
            return (float[]) obj;
        }
        if (obj instanceof List<?> list) {
            float[] result = new float[list.size()];
            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                if (item instanceof Number number) {
                    result[i] = number.floatValue();
                } else {
                    log.warn("无法转换向量元素: {}", item);
                    result[i] = 0.0f;
                }
            }
            return result;
        }
        log.warn("无法转换向量对象: {}", obj.getClass().getName());
        return null;
    }

    /** HNSW 索引项实现。 */
    private static final class FloatArrayItem implements Item<String, float[]>, Serializable {
        private static final long serialVersionUID = 1L;
        private final String id;
        private final float[] vector;

        FloatArrayItem(String id, float[] vector) {
            this.id = id;
            this.vector = vector;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public float[] vector() {
            return vector;
        }

        @Override
        public int dimensions() {
            return vector.length;
        }
    }
}
