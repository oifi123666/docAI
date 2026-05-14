package com.javaee.aiservice.agent;

import com.javaee.aiservice.rag.DocumentVectorizer;
import com.javaee.aiservice.rag.KnowledgeBase;
import com.javaee.aiservice.rag.VectorStore;
import com.javaee.aiservice.agent.execution.event.AgentProgressBroadcaster;
import com.javaee.aiservice.agent.execution.event.AgentProgressEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 知识索引Agent
 * 负责异步管道: 解析 -> 切分 -> 标签/分类 -> 向量化 -> 写库 -> 质量评估。
 * 支持权限隔离、状态查询、失败重试。
 */
@Component
public class KnowledgeIndexAgent {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeIndexAgent.class);
    private static final long JOB_TTL_DAYS = 7L;
    private static final int MAX_JOBS = 500;
    private static final String JOB_KEY_PREFIX = "agent:knowledge:job:";
    private static final String JOB_ALL_ZSET = "agent:knowledge:jobs:all";
    private static final String JOB_USER_ZSET_PREFIX = "agent:knowledge:jobs:user:";

    @Autowired
    private DocumentVectorizer documentVectorizer;

    @Autowired
    private KnowledgeBase knowledgeBase;

    @Autowired
    private VectorStore vectorStore;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private AgentProgressBroadcaster progressBroadcaster;

    private final Map<String, KnowledgeIndexJob> jobs = new ConcurrentHashMap<>();
    private final ExecutorService indexExecutor = Executors.newFixedThreadPool(
            Math.max(2, Math.min(8, Runtime.getRuntime().availableProcessors())),
            runnable -> {
                Thread thread = new Thread(runnable, "knowledge-index-worker");
                thread.setDaemon(true);
                return thread;
            });

    @PreDestroy
    public void shutdown() {
        indexExecutor.shutdownNow();
    }

    /**
     * 同步索引文档（保留兼容旧接口，内部走相同管道但阻塞返回）。
     */
    public Map<String, Object> indexDocument(String documentId, String content, Map<String, Object> metadata) {
        KnowledgeIndexJob job = createJob(documentId, metadata);
        runPipeline(job, content, metadata);
        return job.snapshot();
    }

    /**
     * 异步索引文档，立即返回任务状态。
     */
    public Map<String, Object> indexDocumentAsync(String documentId, String content, Map<String, Object> metadata) {
        KnowledgeIndexJob job = createJob(documentId, metadata);
        scheduleAsync(job, content, sanitizeMetadata(metadata));
        return job.snapshot();
    }

    Future<?> scheduleAsync(KnowledgeIndexJob job, String content, Map<String, Object> metadata) {
        return indexExecutor.submit(() -> runPipeline(job, content, metadata));
    }

    public Map<String, Object> getJobStatus(String jobId) {
        KnowledgeIndexJob job = jobs.get(jobId);
        if (job != null) {
            return job.snapshot();
        }
        Map<String, Object> persisted = loadJobSnapshot(jobId);
        return persisted.isEmpty() ? Map.of("status", "not_found", "jobId", jobId) : persisted;
    }

    public Map<String, Object> retryJob(String jobId, String content, Map<String, Object> metadata) {
        KnowledgeIndexJob job = jobs.get(jobId);
        if (job == null) {
            job = restoreJob(loadJobSnapshot(jobId));
            if (job == null) {
                return Map.of("status", "not_found", "jobId", jobId);
            }
            jobs.put(job.getJobId(), job);
        }
        if (job.getStatus() != KnowledgeIndexStatus.FAILED) {
            return job.snapshot();
        }
        job.setStatus(KnowledgeIndexStatus.PENDING);
        job.setErrorMessage(null);
        saveAndPublish(job, "knowledge_retry", "索引任务已重新排队");
        scheduleAsync(job, content, sanitizeMetadata(metadata));
        return job.snapshot();
    }

    private KnowledgeIndexJob createJob(String documentId, Map<String, Object> metadata) {
        String userId = metadata != null ? String.valueOf(metadata.getOrDefault("userId", "anonymous")) : "anonymous";
        String knowledgeBaseId = metadata != null
                ? String.valueOf(metadata.getOrDefault("knowledgeBaseId", "default"))
                : "default";
        KnowledgeIndexJob job = new KnowledgeIndexJob(UUID.randomUUID().toString(), documentId, userId, knowledgeBaseId);
        jobs.put(job.getJobId(), job);
        saveAndPublish(job, "knowledge_created", "索引任务已创建");
        return job;
    }

    void runPipeline(KnowledgeIndexJob job, String content, Map<String, Object> metadata) {
        try {
            job.incrementAttempts();
            job.setStatus(KnowledgeIndexStatus.PARSING);
            job.setProgress(10);
            saveAndPublish(job, "knowledge_progress", "开始解析文档");
            if (content == null || content.isBlank()) {
                throw new IllegalArgumentException("文档内容为空");
            }
            String hash = computeHash(content);
            job.setContentHash(hash);
            saveAndPublish(job, "knowledge_progress", "完成内容 hash 计算");

            // 重复文档检测：若已索引且 hash 未变更，则跳过向量化与写库。
            Map<String, Object> existingMetadata = knowledgeBase.getDocumentMetadata(job.getDocumentId());
            String previousHash = existingMetadata != null ? String.valueOf(existingMetadata.getOrDefault("contentHash", "")) : "";
            if (!previousHash.isBlank() && previousHash.equals(hash)) {
                job.setReused(true);
                job.setProgress(100);
                job.setStatus(KnowledgeIndexStatus.INDEXED);
                Map<String, Object> quality = evaluateQuality(job.getDocumentId(), content);
                quality.put("reused", true);
                job.setQuality(quality);
                saveAndPublish(job, "knowledge_indexed", "内容未变更，复用现有索引");
                log.info("文档内容未变更，复用现有索引: documentId={}, jobId={}", job.getDocumentId(), job.getJobId());
                return;
            }

            Map<String, Object> enriched = sanitizeMetadata(metadata);
            enriched.putIfAbsent("userId", job.getUserId());
            enriched.putIfAbsent("knowledgeBaseId", job.getKnowledgeBaseId());
            enriched.put("tags", String.join(",", generateTags(content)));
            enriched.put("category", classifyDocument(content));
            enriched.put("contentHash", hash);
            job.setProgress(40);
            saveAndPublish(job, "knowledge_progress", "完成元数据增强");

            job.setStatus(KnowledgeIndexStatus.EMBEDDING);
            saveAndPublish(job, "knowledge_progress", "开始向量化与写库");
            // 若旧文档存在且 hash 不同，先清理旧分段，避免索引污染。
            if (!previousHash.isBlank()) {
                try {
                    knowledgeBase.removeDocument(job.getDocumentId());
                } catch (Exception removalError) {
                    log.warn("清理旧文档失败，继续重新索引: {}", removalError.getMessage());
                }
            }
            knowledgeBase.addDocument(job.getDocumentId(), content, enriched);
            job.setProgress(85);
            saveAndPublish(job, "knowledge_progress", "完成索引写入");

            job.setStatus(KnowledgeIndexStatus.INDEXED);
            job.setQuality(evaluateQuality(job.getDocumentId(), content));
            job.setProgress(100);
            saveAndPublish(job, "knowledge_indexed", "文档索引完成");
            log.info("文档索引完成: documentId={}, jobId={}", job.getDocumentId(), job.getJobId());
        } catch (Exception e) {
            log.error("文档索引失败: jobId={}", job.getJobId(), e);
            job.setStatus(KnowledgeIndexStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            saveAndPublish(job, "knowledge_failed", "文档索引失败: " + e.getMessage());
        }
    }

    /**
     * 计算内容 SHA-256 hash 用于重复检测。
     */
    String computeHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return Integer.toHexString(content.hashCode());
        }
    }

    /**
     * 列出索引任务，按 userId 过滤；管理员场景下传 null 返回全部。
     * 任务按 updatedAt 倒序排列。
     */
    public List<Map<String, Object>> listJobs(String userId, String knowledgeBaseId) {
        List<Map<String, Object>> persisted = listPersistedJobs(userId, knowledgeBaseId);
        if (!persisted.isEmpty()) {
            return persisted;
        }
        return jobs.values().stream()
                .filter(job -> userId == null || userId.equals(job.getUserId()))
                .filter(job -> knowledgeBaseId == null || knowledgeBaseId.equals(job.getKnowledgeBaseId()))
                .sorted(Comparator.comparingLong(KnowledgeIndexJob::getUpdatedAt).reversed())
                .map(KnowledgeIndexJob::snapshot)
                .toList();
    }

    /**
     * 删除一条索引任务记录。仅删除任务追踪数据，不影响向量库内容。
     */
    public boolean deleteJob(String jobId) {
        return jobs.remove(jobId) != null || deletePersistedJob(jobId);
    }

    private void saveAndPublish(KnowledgeIndexJob job, String eventType, String message) {
        if (job == null) {
            return;
        }
        Map<String, Object> snapshot = job.snapshot();
        persistJob(snapshot);
        if (progressBroadcaster != null) {
            AgentProgressEvent event = AgentProgressEvent.of(eventType, job.getUserId(), job.getStatus().name(), message);
            event.setJobId(job.getJobId());
            event.setProgress(job.getProgress());
            event.setPayload(snapshot);
            progressBroadcaster.publish(event);
        }
    }

    private void persistJob(Map<String, Object> snapshot) {
        if (redisTemplate == null || snapshot == null || snapshot.get("jobId") == null) {
            return;
        }
        try {
            String jobId = String.valueOf(snapshot.get("jobId"));
            String key = JOB_KEY_PREFIX + jobId;
            long score = asLong(snapshot.get("updatedAt"));
            redisTemplate.opsForValue().set(key, snapshot, JOB_TTL_DAYS, TimeUnit.DAYS);
            redisTemplate.opsForZSet().add(JOB_ALL_ZSET, jobId, score);
            Object userId = snapshot.get("userId");
            if (userId != null) {
                redisTemplate.opsForZSet().add(JOB_USER_ZSET_PREFIX + userId, jobId, score);
            }
        } catch (Exception ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadJobSnapshot(String jobId) {
        if (redisTemplate == null || jobId == null) {
            return Map.of();
        }
        try {
            Object stored = redisTemplate.opsForValue().get(JOB_KEY_PREFIX + jobId);
            if (stored instanceof Map<?, ?> map) {
                return new LinkedHashMap<>((Map<String, Object>) map);
            }
        } catch (Exception ignored) {
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private KnowledgeIndexJob restoreJob(Map<String, Object> snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            return null;
        }
        KnowledgeIndexJob job = new KnowledgeIndexJob(
                String.valueOf(snapshot.get("jobId")),
                String.valueOf(snapshot.get("documentId")),
                String.valueOf(snapshot.get("userId")),
                String.valueOf(snapshot.getOrDefault("knowledgeBaseId", "default"))
        );
        Object status = snapshot.get("status");
        if (status != null) {
            try {
                job.setStatus(KnowledgeIndexStatus.valueOf(status.toString()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        job.setAttempts((int) asLong(snapshot.get("attempts")));
        job.setProgress((int) asLong(snapshot.get("progress")));
        job.setContentHash(snapshot.get("contentHash") == null ? null : String.valueOf(snapshot.get("contentHash")));
        job.setReused(Boolean.parseBoolean(String.valueOf(snapshot.getOrDefault("reused", false))));
        job.setErrorMessage(snapshot.get("errorMessage") == null ? null : String.valueOf(snapshot.get("errorMessage")));
        if (snapshot.get("quality") instanceof Map<?, ?> quality) {
            job.setQuality(new LinkedHashMap<>((Map<String, Object>) quality));
        }
        return job;
    }

    private List<Map<String, Object>> listPersistedJobs(String userId, String knowledgeBaseId) {
        if (redisTemplate == null) {
            return List.of();
        }
        try {
            String zset = userId == null ? JOB_ALL_ZSET : JOB_USER_ZSET_PREFIX + userId;
            Set<Object> ids = redisTemplate.opsForZSet().reverseRange(zset, 0, MAX_JOBS - 1);
            if (ids == null || ids.isEmpty()) {
                return List.of();
            }
            List<Map<String, Object>> snapshots = new ArrayList<>();
            for (Object id : ids) {
                Map<String, Object> snapshot = loadJobSnapshot(String.valueOf(id));
                if (!snapshot.isEmpty()
                        && (knowledgeBaseId == null || knowledgeBaseId.equals(String.valueOf(snapshot.get("knowledgeBaseId"))))) {
                    snapshots.add(snapshot);
                }
            }
            snapshots.sort((a, b) -> Long.compare(asLong(b.get("updatedAt")), asLong(a.get("updatedAt"))));
            return snapshots;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private boolean deletePersistedJob(String jobId) {
        if (redisTemplate == null || jobId == null) {
            return false;
        }
        try {
            Map<String, Object> snapshot = loadJobSnapshot(jobId);
            redisTemplate.delete(JOB_KEY_PREFIX + jobId);
            redisTemplate.opsForZSet().remove(JOB_ALL_ZSET, jobId);
            Object userId = snapshot.get("userId");
            if (userId != null) {
                redisTemplate.opsForZSet().remove(JOB_USER_ZSET_PREFIX + userId, jobId);
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private long asLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return value == null ? 0L : Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    public List<Map<String, Object>> searchKnowledge(String query, int topK, String userId, String knowledgeBaseId) {
        log.info("搜索知识库: query={}, userId={}", query, userId);
        try {
            float[] queryVector = documentVectorizer.vectorize(query);
            List<Map<String, Object>> results = vectorStore.search(queryVector, Math.max(1, Math.min(topK, 50)));
            return results.stream()
                    .filter(item -> matchesOwner(item, userId, knowledgeBaseId))
                    .toList();
        } catch (Exception e) {
            log.error("知识库搜索失败", e);
            throw new RuntimeException("知识库搜索失败: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> deleteIndex(String documentId) {
        log.info("删除文档索引: documentId={}", documentId);
        try {
            vectorStore.delete(documentId);
            knowledgeBase.removeDocument(documentId);
            return Map.of(
                "status", "success",
                "documentId", documentId,
                "message", "索引删除成功"
            );
        } catch (Exception e) {
            log.error("删除索引失败", e);
            return Map.of(
                "status", "error",
                "documentId", documentId,
                "message", "索引删除失败: " + e.getMessage()
            );
        }
    }

    private boolean matchesOwner(Map<String, Object> item, String userId, String knowledgeBaseId) {
        if (userId == null) {
            return true;
        }
        Object owner = item.getOrDefault("userId", "");
        Object kb = item.getOrDefault("knowledgeBaseId", "default");
        return userId.equals(String.valueOf(owner))
                && (knowledgeBaseId == null || knowledgeBaseId.equals(String.valueOf(kb)));
    }

    private Map<String, Object> sanitizeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return new HashMap<>();
        }
        try {
            metadata.put("__probe", 1);
            metadata.remove("__probe");
            return metadata;
        } catch (UnsupportedOperationException e) {
            return new HashMap<>(metadata);
        }
    }

    /**
     * 生成文档标签：先用关键词命中，再补充少量启发式标签。
     */
    List<String> generateTags(String content) {
        Set<String> tags = new LinkedHashSet<>();
        String lower = content.toLowerCase(Locale.ROOT);
        Map<String, List<String>> tagDict = new LinkedHashMap<>();
        tagDict.put("AI", List.of("ai", "人工智能", "大模型", "llm", "agent"));
        tagDict.put("文档处理", List.of("文档", "解析", "ocr"));
        tagDict.put("代码", List.of("代码", "源码", "function", "class "));
        tagDict.put("分析报告", List.of("分析", "报告", "结论", "kpi"));
        tagDict.put("会议", List.of("会议", "纪要", "议题"));
        tagDict.put("教程", List.of("教程", "指南", "tutorial", "how to"));
        for (Map.Entry<String, List<String>> entry : tagDict.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (lower.contains(keyword)) {
                    tags.add(entry.getKey());
                    break;
                }
            }
        }
        if (tags.isEmpty()) {
            tags.add("通用");
        }
        return new ArrayList<>(tags);
    }

    String classifyDocument(String content) {
        String lower = content.toLowerCase(Locale.ROOT);
        if (containsAny(lower, "代码", "编程", "开发", "function", "class ")) {
            return "技术文档";
        }
        if (containsAny(lower, "报告", "分析", "kpi", "数据")) {
            return "分析报告";
        }
        if (containsAny(lower, "会议", "纪要", "议题")) {
            return "会议记录";
        }
        if (containsAny(lower, "教程", "指南", "how to", "tutorial")) {
            return "教程";
        }
        return "其他";
    }

    private boolean containsAny(String haystack, String... needles) {
        return Arrays.stream(needles).anyMatch(haystack::contains);
    }

    Map<String, Object> evaluateQuality(String documentId, String content) {
        Map<String, Object> quality = new LinkedHashMap<>();
        int length = content.length();
        int chunks = Math.max(1, length / 500);
        quality.put("documentLength", length);
        quality.put("estimatedChunks", chunks);
        quality.put("emptyContent", length == 0);
        quality.put("score", length == 0 ? 0 : Math.min(100, 60 + Math.min(40, chunks)));
        quality.put("documentId", documentId);
        return quality;
    }
}
