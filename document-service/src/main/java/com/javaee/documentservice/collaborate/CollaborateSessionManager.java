package com.javaee.documentservice.collaborate;

import com.javaee.common.utils.RedisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class CollaborateSessionManager {

    private static final Logger log = LoggerFactory.getLogger(CollaborateSessionManager.class);

    private static final String REDIS_DOC_USERS_PREFIX = "collaborate:doc:users:";
    private static final String REDIS_USER_DOC_PREFIX = "collaborate:user:doc:";
    private static final long SESSION_TIMEOUT_MINUTES = 30;

    private final Map<String, Set<String>> localDocumentUsers = new ConcurrentHashMap<>();
    private final Map<String, String> localUserDocuments = new ConcurrentHashMap<>();

    @Autowired
    private RedisUtils redisUtils;

    public void userJoinDocument(String documentId, String userId, String userName) {
        localDocumentUsers.computeIfAbsent(documentId, k -> ConcurrentHashMap.newKeySet()).add(userId);
        localUserDocuments.put(userId, documentId);

        String redisKey = REDIS_DOC_USERS_PREFIX + documentId;
        redisUtils.hSet(redisKey, userId, userName);
        redisUtils.expire(redisKey, SESSION_TIMEOUT_MINUTES, TimeUnit.MINUTES);

        String userDocKey = REDIS_USER_DOC_PREFIX + userId;
        redisUtils.set(userDocKey, documentId, SESSION_TIMEOUT_MINUTES, TimeUnit.MINUTES);

        log.info("用户加入文档协同: userId={}, userName={}, documentId={}, 当前在线人数={}",
                userId, userName, documentId, getOnlineUserCount(documentId));
    }

    public void userLeaveDocument(String documentId, String userId) {
        Set<String> users = localDocumentUsers.get(documentId);
        if (users != null) {
            users.remove(userId);
            if (users.isEmpty()) {
                localDocumentUsers.remove(documentId);
            }
        }
        localUserDocuments.remove(userId);

        String redisKey = REDIS_DOC_USERS_PREFIX + documentId;
        redisUtils.hDelete(redisKey, userId);

        log.info("用户离开文档协同: userId={}, documentId={}, 当前在线人数={}",
                userId, documentId, getOnlineUserCount(documentId));
    }

    public Set<String> getDocumentOnlineUsers(String documentId) {
        Set<String> localUsers = localDocumentUsers.get(documentId);
        if (localUsers != null && !localUsers.isEmpty()) {
            return localUsers;
        }

        Map<Object, Object> redisUsers = redisUtils.hGetAll(REDIS_DOC_USERS_PREFIX + documentId);
        if (redisUsers != null) {
            Set<String> userIds = ConcurrentHashMap.newKeySet();
            for (Object key : redisUsers.keySet()) {
                userIds.add(key.toString());
            }
            return userIds;
        }
        return Set.of();
    }

    public int getOnlineUserCount(String documentId) {
        Set<String> localUsers = localDocumentUsers.get(documentId);
        if (localUsers != null) {
            return localUsers.size();
        }
        return 0;
    }

    public String getUserCurrentDocument(String userId) {
        String docId = localUserDocuments.get(userId);
        if (docId != null) {
            return docId;
        }
        Object redisDocId = redisUtils.get(REDIS_USER_DOC_PREFIX + userId);
        return redisDocId != null ? redisDocId.toString() : null;
    }

    public void refreshSession(String documentId) {
        String redisKey = REDIS_DOC_USERS_PREFIX + documentId;
        redisUtils.expire(redisKey, SESSION_TIMEOUT_MINUTES, TimeUnit.MINUTES);
    }
}