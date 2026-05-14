package com.javaee.aiservice.async;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaee.aiservice.config.AiRabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Stores async job state and publishes model requests to RabbitMQ.
 */
@Service
public class AsyncAIJobService {

    private static final String JOB_PREFIX = "ai:async:job:";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${ai.async.job-expiry-hours:24}")
    private long jobExpiryHours;

    public AsyncAIJobVO submit(String type, Object payload, String model, String userId) {
        String jobId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();

        AsyncAIJobVO job = new AsyncAIJobVO();
        job.setJobId(jobId);
        job.setType(type);
        job.setModel(model);
        job.setUserId(userId);
        job.setStatus("queued");
        job.setSubmittedAt(now);
        save(job);

        AsyncAIJobMessage message = new AsyncAIJobMessage();
        message.setJobId(jobId);
        message.setType(type);
        message.setModel(model);
        message.setUserId(userId);
        message.setCreatedAt(now);
        message.setPayload(toMap(payload));

        rabbitTemplate.convertAndSend(
                AiRabbitMQConfig.AI_EXCHANGE,
                AiRabbitMQConfig.AI_MODEL_REQUEST_ROUTING_KEY,
                message
        );

        return job;
    }

    public AsyncAIJobVO getJob(String jobId, String userId, boolean admin) {
        AsyncAIJobVO job = load(jobId);
        if (job == null) {
            throw new IllegalArgumentException("异步任务不存在: " + jobId);
        }
        if (!admin && !userId.equals(job.getUserId())) {
            throw new SecurityException("无权访问该异步任务");
        }
        return job;
    }

    public void markRunning(String jobId) {
        AsyncAIJobVO job = requireJob(jobId);
        job.setStatus("running");
        job.setStartedAt(System.currentTimeMillis());
        save(job);
    }

    public void markSuccess(String jobId, Object result) {
        AsyncAIJobVO job = requireJob(jobId);
        job.setStatus("success");
        job.setResult(result);
        job.setFinishedAt(System.currentTimeMillis());
        save(job);
    }

    public void markFailed(String jobId, String error) {
        AsyncAIJobVO job = requireJob(jobId);
        job.setStatus("failed");
        job.setError(error);
        job.setFinishedAt(System.currentTimeMillis());
        save(job);
    }

    private AsyncAIJobVO requireJob(String jobId) {
        AsyncAIJobVO job = load(jobId);
        if (job == null) {
            throw new IllegalArgumentException("异步任务不存在: " + jobId);
        }
        return job;
    }

    private AsyncAIJobVO load(String jobId) {
        Object value = redisTemplate.opsForValue().get(JOB_PREFIX + jobId);
        if (value instanceof AsyncAIJobVO job) {
            return job;
        }
        return objectMapper.convertValue(value, AsyncAIJobVO.class);
    }

    private void save(AsyncAIJobVO job) {
        redisTemplate.opsForValue().set(
                JOB_PREFIX + job.getJobId(),
                job,
                Duration.ofHours(jobExpiryHours)
        );
    }

    private Map<String, Object> toMap(Object payload) {
        if (payload == null) {
            return Map.of();
        }
        return objectMapper.convertValue(payload, new TypeReference<>() {});
    }
}
