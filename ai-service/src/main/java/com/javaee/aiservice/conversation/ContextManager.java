package com.javaee.aiservice.conversation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 上下文管理器
 * 管理对话上下文和状态
 * 支持上下文的保存、更新和清理
 */
@Component
public class ContextManager {

    private static final Logger log = LoggerFactory.getLogger(ContextManager.class);
    private static final String CONTEXT_PREFIX = "ctx:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取对话上下文
     * @param conversationId 对话ID
     * @return 上下文信息
     */
    public Map<String, Object> getContext(String conversationId) {
        String key = CONTEXT_PREFIX + conversationId;
        
        Map<Object, Object> hash = redisTemplate.opsForHash().entries(key);
        Map<String, Object> context = new HashMap<>();
        for (Map.Entry<Object, Object> entry : hash.entrySet()) {
            context.put(entry.getKey().toString(), entry.getValue());
        }
        
        log.debug("获取上下文: conversationId={}, size={}", conversationId, context.size());
        return context;
    }

    /**
     * 更新对话上下文
     * @param conversationId 对话ID
     * @param updates 更新内容
     */
    public void updateContext(String conversationId, Map<String, Object> updates) {
        if (updates == null || updates.isEmpty()) {
            return;
        }

        String key = CONTEXT_PREFIX + conversationId;
        redisTemplate.opsForHash().putAll(key, updates);
        
        log.debug("更新上下文: conversationId={}, updates={}", conversationId, updates.size());
    }

    /**
     * 设置上下文字段
     * @param conversationId 对话ID
     * @param key 字段名
     * @param value 字段值
     */
    public void setContextValue(String conversationId, String key, Object value) {
        String contextKey = CONTEXT_PREFIX + conversationId;
        redisTemplate.opsForHash().put(contextKey, key, value);
        
        log.debug("设置上下文字段: conversationId={}, key={}", conversationId, key);
    }

    /**
     * 获取上下文字段
     * @param conversationId 对话ID
     * @param key 字段名
     * @return 字段值
     */
    public Object getContextValue(String conversationId, String key) {
        String contextKey = CONTEXT_PREFIX + conversationId;
        return redisTemplate.opsForHash().get(contextKey, key);
    }

    /**
     * 清理对话上下文
     * @param conversationId 对话ID
     */
    public void clearContext(String conversationId) {
        String key = CONTEXT_PREFIX + conversationId;
        redisTemplate.delete(key);
        
        log.debug("清理上下文: conversationId={}", conversationId);
    }

    /**
     * 合并上下文
     * @param conversationId 对话ID
     * @param newContext 新上下文
     */
    public void mergeContext(String conversationId, Map<String, Object> newContext) {
        if (newContext == null || newContext.isEmpty()) {
            return;
        }

        Map<String, Object> currentContext = getContext(conversationId);
        currentContext.putAll(newContext);
        
        String key = CONTEXT_PREFIX + conversationId;
        redisTemplate.delete(key);
        redisTemplate.opsForHash().putAll(key, currentContext);
        
        log.debug("合并上下文: conversationId={}, size={}", conversationId, currentContext.size());
    }
}
