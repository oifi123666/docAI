package com.javaee.aiservice.agent.execution.approval;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side approval store for destructive Agent tools.
 */
@Service
public class AgentApprovalService {

    private static final String PREFIX = "agent:approval:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${ai.agent.approval-expiry-seconds:300}")
    private long approvalExpirySeconds;

    public ApprovalChallenge createChallenge(String userId, String toolName, Map<String, Object> params) {
        String token = UUID.randomUUID().toString();
        String fingerprint = fingerprint(userId, toolName, params);
        redisTemplate.opsForValue().set(PREFIX + token, fingerprint, Duration.ofSeconds(approvalExpirySeconds));
        return new ApprovalChallenge(token, System.currentTimeMillis() + approvalExpirySeconds * 1000);
    }

    public ApprovalResult verifyAndConsumeDetailed(String token, String userId, String toolName, Map<String, Object> params) {
        if (token == null || token.isBlank()) {
            return ApprovalResult.MISSING;
        }
        String key = PREFIX + token;
        Object expected = redisTemplate.opsForValue().get(key);
        if (expected == null) {
            return ApprovalResult.EXPIRED_OR_USED;
        }
        String actual = fingerprint(userId, toolName, params);
        if (!expected.toString().equals(actual)) {
            return ApprovalResult.PARAMS_MISMATCH;
        }
        redisTemplate.delete(key);
        return ApprovalResult.OK;
    }

    public boolean verifyAndConsume(String token, String userId, String toolName, Map<String, Object> params) {
        return verifyAndConsumeDetailed(token, userId, toolName, params) == ApprovalResult.OK;
    }

    public boolean cancel(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.delete(PREFIX + token));
    }

    private String fingerprint(String userId, String toolName, Map<String, Object> params) {
        String objectName = String.valueOf(params.getOrDefault("objectName", ""));
        String targetVersionId = String.valueOf(params.getOrDefault("targetVersionId", ""));
        String recycleId = String.valueOf(params.getOrDefault("recycleId", ""));
        String raw = userId + "|" + toolName + "|" + objectName + "|" + targetVersionId + "|" + recycleId;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("生成审批指纹失败", e);
        }
    }

    public record ApprovalChallenge(String token, long expiresAt) {
    }

    public enum ApprovalResult {
        OK,
        MISSING,
        EXPIRED_OR_USED,
        PARAMS_MISMATCH
    }
}
