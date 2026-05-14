package com.javaee.aiservice.agent;

import com.javaee.aiservice.rag.DocumentVectorizer;
import com.javaee.aiservice.rag.KnowledgeBase;
import com.javaee.aiservice.rag.VectorStore;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeIndexAgentTest {

    @Test
    void runPipelineMarksJobIndexedAndAttachesQuality() {
        KnowledgeIndexAgent agent = new KnowledgeIndexAgent();
        DocumentVectorizer vectorizer = mock(DocumentVectorizer.class);
        KnowledgeBase knowledgeBase = mock(KnowledgeBase.class);
        VectorStore vectorStore = mock(VectorStore.class);
        ReflectionTestUtils.setField(agent, "documentVectorizer", vectorizer);
        ReflectionTestUtils.setField(agent, "knowledgeBase", knowledgeBase);
        ReflectionTestUtils.setField(agent, "vectorStore", vectorStore);

        KnowledgeIndexJob job = new KnowledgeIndexJob("job-1", "doc-1", "user-1", "kb-1");
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userId", "user-1");

        agent.runPipeline(job, "这是一份技术文档，包含 AI 相关代码示例", metadata);

        assertThat(job.getStatus()).isEqualTo(KnowledgeIndexStatus.INDEXED);
        assertThat(job.getQuality()).containsKey("score");
        assertThat(job.getAttempts()).isEqualTo(1);
    }

    @Test
    void runPipelineMarksFailedWhenContentEmpty() {
        KnowledgeIndexAgent agent = new KnowledgeIndexAgent();
        ReflectionTestUtils.setField(agent, "knowledgeBase", mock(KnowledgeBase.class));
        ReflectionTestUtils.setField(agent, "vectorStore", mock(VectorStore.class));
        ReflectionTestUtils.setField(agent, "documentVectorizer", mock(DocumentVectorizer.class));

        KnowledgeIndexJob job = new KnowledgeIndexJob("job-2", "doc-2", "user-1", "kb-1");
        agent.runPipeline(job, "", new HashMap<>());

        assertThat(job.getStatus()).isEqualTo(KnowledgeIndexStatus.FAILED);
        assertThat(job.getErrorMessage()).contains("文档内容为空");
    }

    @Test
    void searchKnowledgeFiltersByOwnerAndKnowledgeBase() {
        KnowledgeIndexAgent agent = new KnowledgeIndexAgent();
        DocumentVectorizer vectorizer = mock(DocumentVectorizer.class);
        VectorStore vectorStore = mock(VectorStore.class);
        ReflectionTestUtils.setField(agent, "documentVectorizer", vectorizer);
        ReflectionTestUtils.setField(agent, "vectorStore", vectorStore);
        ReflectionTestUtils.setField(agent, "knowledgeBase", mock(KnowledgeBase.class));
        when(vectorizer.vectorize(any())).thenReturn(new float[]{0.1f, 0.2f});
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("userId", "user-1");
        a.put("knowledgeBaseId", "kb-1");
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("userId", "user-2");
        b.put("knowledgeBaseId", "kb-1");
        when(vectorStore.search(any(), anyInt())).thenReturn(List.of(a, b));

        List<Map<String, Object>> filtered = agent.searchKnowledge("query", 5, "user-1", "kb-1");

        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).get("userId")).isEqualTo("user-1");
    }

    @Test
    void retryJobRescheduleFailedJob() {
        KnowledgeIndexAgent agent = new KnowledgeIndexAgent();
        KnowledgeBase knowledgeBase = mock(KnowledgeBase.class);
        ReflectionTestUtils.setField(agent, "knowledgeBase", knowledgeBase);
        ReflectionTestUtils.setField(agent, "vectorStore", mock(VectorStore.class));
        ReflectionTestUtils.setField(agent, "documentVectorizer", mock(DocumentVectorizer.class));
        // 第一次失败
        doThrow(new RuntimeException("boom"))
                .when(knowledgeBase).addDocument(any(), any(), any());

        Map<String, Object> firstAttempt = agent.indexDocument("doc-1", "content", new HashMap<>());

        assertThat(firstAttempt.get("status")).isEqualTo("FAILED");
        String jobId = (String) firstAttempt.get("jobId");

        // 第二次成功
        org.mockito.Mockito.reset(knowledgeBase);
        Map<String, Object> retried = agent.retryJob(jobId, "content", new HashMap<>());
        assertThat(retried.get("status").toString()).isIn("PENDING", "INDEXED", "EMBEDDING", "PARSING");
    }

    @Test
    void runPipelineReusesIndexWhenContentHashUnchanged() {
        KnowledgeIndexAgent agent = new KnowledgeIndexAgent();
        KnowledgeBase knowledgeBase = mock(KnowledgeBase.class);
        ReflectionTestUtils.setField(agent, "knowledgeBase", knowledgeBase);
        ReflectionTestUtils.setField(agent, "vectorStore", mock(VectorStore.class));
        ReflectionTestUtils.setField(agent, "documentVectorizer", mock(DocumentVectorizer.class));
        String content = "重复内容";
        String hash = agent.computeHash(content);
        Map<String, Object> existing = new HashMap<>();
        existing.put("contentHash", hash);
        when(knowledgeBase.getDocumentMetadata("doc-1")).thenReturn(existing);

        KnowledgeIndexJob job = new KnowledgeIndexJob("job-3", "doc-1", "user-1", "kb-1");
        agent.runPipeline(job, content, new HashMap<>());

        assertThat(job.getStatus()).isEqualTo(KnowledgeIndexStatus.INDEXED);
        assertThat(job.isReused()).isTrue();
        assertThat(job.getProgress()).isEqualTo(100);
        assertThat(job.getQuality()).containsEntry("reused", true);
        org.mockito.Mockito.verify(knowledgeBase, org.mockito.Mockito.never()).addDocument(any(), any(), any());
    }

    @Test
    void runPipelineRebuildsIndexWhenHashChanged() {
        KnowledgeIndexAgent agent = new KnowledgeIndexAgent();
        KnowledgeBase knowledgeBase = mock(KnowledgeBase.class);
        ReflectionTestUtils.setField(agent, "knowledgeBase", knowledgeBase);
        ReflectionTestUtils.setField(agent, "vectorStore", mock(VectorStore.class));
        ReflectionTestUtils.setField(agent, "documentVectorizer", mock(DocumentVectorizer.class));
        Map<String, Object> existing = new HashMap<>();
        existing.put("contentHash", "stale-hash");
        when(knowledgeBase.getDocumentMetadata("doc-2")).thenReturn(existing);

        KnowledgeIndexJob job = new KnowledgeIndexJob("job-4", "doc-2", "user-1", "kb-1");
        agent.runPipeline(job, "新内容", new HashMap<>());

        assertThat(job.getStatus()).isEqualTo(KnowledgeIndexStatus.INDEXED);
        assertThat(job.isReused()).isFalse();
        assertThat(job.getProgress()).isEqualTo(100);
        org.mockito.Mockito.verify(knowledgeBase).removeDocument("doc-2");
        org.mockito.Mockito.verify(knowledgeBase).addDocument(eq("doc-2"), eq("新内容"), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void listJobsFiltersByOwnerAndDeleteRemovesEntry() {
        KnowledgeIndexAgent agent = new KnowledgeIndexAgent();
        ReflectionTestUtils.setField(agent, "knowledgeBase", mock(KnowledgeBase.class));
        ReflectionTestUtils.setField(agent, "vectorStore", mock(VectorStore.class));
        ReflectionTestUtils.setField(agent, "documentVectorizer", mock(DocumentVectorizer.class));

        Map<String, KnowledgeIndexJob> jobs = (Map<String, KnowledgeIndexJob>)
                ReflectionTestUtils.getField(agent, "jobs");
        KnowledgeIndexJob a = new KnowledgeIndexJob("job-a", "doc-a", "user-1", "kb-1");
        KnowledgeIndexJob b = new KnowledgeIndexJob("job-b", "doc-b", "user-2", "kb-1");
        jobs.put(a.getJobId(), a);
        jobs.put(b.getJobId(), b);

        assertThat(agent.listJobs("user-1", "kb-1")).hasSize(1);
        assertThat(agent.listJobs(null, null)).hasSize(2);
        assertThat(agent.listJobs("user-1", "other-kb")).isEmpty();
        assertThat(agent.deleteJob("job-a")).isTrue();
        assertThat(agent.listJobs(null, null)).hasSize(1);
    }
}
