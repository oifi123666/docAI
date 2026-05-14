package com.javaee.aiservice.conversation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对话管理器
 * 管理对话生命周期和消息历史
 * 支持多轮对话和上下文管理
 */
@Component
public class ConversationManager {

    private static final Logger log = LoggerFactory.getLogger(ConversationManager.class);
    private static final String CONVERSATION_PREFIX = "conv:";
    private static final String USER_PREFIX = "user:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${ai.conversation.max-messages:100}")
    private int maxMessages;

    @Value("${ai.conversation.expiry-hours:24}")
    private int expiryHours;

    /**
     * 创建新对话
     * @param userId 用户ID
     * @return 对话ID
     */
    public String createConversation(String userId) {
        String conversationId = UUID.randomUUID().toString();
        String key = CONVERSATION_PREFIX + conversationId;

        Map<String, Object> conversation = new HashMap<>();
        conversation.put("userId", userId);
        conversation.put("createdAt", System.currentTimeMillis());
        conversation.put("updatedAt", System.currentTimeMillis());
        conversation.put("messages", new ArrayList<String>());

        redisTemplate.opsForHash().putAll(key, conversation);
        redisTemplate.expire(key, java.time.Duration.ofHours(expiryHours));

        log.info("创建新对话: userId={}, conversationId={}", userId, conversationId);
        return conversationId;
    }

    /**
     * 添加消息到对话
     * @param conversationId 对话ID
     * @param userMessage 用户消息
     * @param assistantMessage 助手消息
     */
    public void addMessage(String conversationId, String userMessage, String assistantMessage) {
        String key = CONVERSATION_PREFIX + conversationId;
        
        @SuppressWarnings("unchecked")
        List<String> messages = (List<String>) redisTemplate.opsForHash().get(key, "messages");
        if (messages == null) {
            messages = new ArrayList<>();
        }

        messages.add("User: " + userMessage);
        messages.add("Assistant: " + assistantMessage);

        if (messages.size() > maxMessages) {
            messages = messages.subList(messages.size() - maxMessages, messages.size());
        }

        redisTemplate.opsForHash().put(key, "messages", messages);
        redisTemplate.opsForHash().put(key, "updatedAt", System.currentTimeMillis());
        redisTemplate.expire(key, java.time.Duration.ofHours(expiryHours));

        log.debug("添加消息到对话: conversationId={}, messageCount={}", conversationId, messages.size());
    }

    /**
     * 获取对话历史
     * @param conversationId 对话ID
     * @return 消息列表
     */
    public List<String> getConversationHistory(String conversationId) {
        String key = CONVERSATION_PREFIX + conversationId;
        
        @SuppressWarnings("unchecked")
        List<String> messages = (List<String>) redisTemplate.opsForHash().get(key, "messages");
        return messages != null ? messages : Collections.emptyList();
    }

    /**
     * 删除对话
     * @param conversationId 对话ID
     */
    public void deleteConversation(String conversationId) {
        String key = CONVERSATION_PREFIX + conversationId;
        redisTemplate.delete(key);
        log.info("删除对话: conversationId={}", conversationId);
    }

    public void addMessageForUser(String conversationId, String userId, String userMessage, String assistantMessage) {
        assertOwner(conversationId, userId);
        addMessage(conversationId, userMessage, assistantMessage);
    }

    public List<String> getConversationHistoryForUser(String conversationId, String userId) {
        assertOwner(conversationId, userId);
        return getConversationHistory(conversationId);
    }

    public void deleteConversationForUser(String conversationId, String userId) {
        assertOwner(conversationId, userId);
        deleteConversation(conversationId);
    }

    public void assertOwner(String conversationId, String userId) {
        if (conversationId == null || conversationId.isBlank()) {
            throw new IllegalArgumentException("对话ID不能为空");
        }
        String key = CONVERSATION_PREFIX + conversationId;
        Object owner = redisTemplate.opsForHash().get(key, "userId");
        if (owner == null) {
            throw new IllegalArgumentException("对话不存在");
        }
        if (!String.valueOf(owner).equals(userId)) {
            throw new SecurityException("无权访问该对话");
        }
    }

    /**
     * 获取用户的所有对话
     * @param userId 用户ID
     * @return 对话ID列表
     */
    public List<String> getUserConversations(String userId) {
        try {
            List<String> conversationIds = new ArrayList<>();
            ScanOptions options = ScanOptions.scanOptions().match(CONVERSATION_PREFIX + "*").count(200).build();
            try (var cursor = redisTemplate.getConnectionFactory().getConnection().scan(options)) {
                while (cursor.hasNext()) {
                    String key = new String(cursor.next(), StandardCharsets.UTF_8);
                    String convUserId = (String) redisTemplate.opsForHash().get(key, "userId");
                if (userId.equals(convUserId)) {
                    conversationIds.add(key.substring(CONVERSATION_PREFIX.length()));
                }
            }
            }
            return conversationIds;
        } catch (Exception e) {
            log.warn("获取用户对话列表失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取对话信息
     * @param conversationId 对话ID
     * @return 对话信息
     */
    public Map<String, Object> getConversationInfo(String conversationId) {
        String key = CONVERSATION_PREFIX + conversationId;
        Map<Object, Object> hash = redisTemplate.opsForHash().entries(key);
        
        Map<String, Object> info = new HashMap<>();
        for (Map.Entry<Object, Object> entry : hash.entrySet()) {
            info.put(entry.getKey().toString(), entry.getValue());
        }
        return info;
    }
}
