package com.javaee.documentservice.collaborate;

import com.javaee.common.utils.RedisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class CursorSyncHandler {

    private static final Logger log = LoggerFactory.getLogger(CursorSyncHandler.class);

    private static final String REDIS_CURSOR_PREFIX = "collaborate:cursor:";
    private static final long CURSOR_EXPIRE_MINUTES = 5;

    @Autowired
    private RedisUtils redisUtils;

    public void updateCursor(CursorPosition cursor) {
        String cursorKey = REDIS_CURSOR_PREFIX + cursor.getDocumentId();
        String fieldKey = cursor.getUserId();

        String cursorValue = cursor.getUserName() + ":" + cursor.getFrom() + ":" + cursor.getTo() + ":" + cursor.getTimestamp();
        redisUtils.hSet(cursorKey, fieldKey, cursorValue);
        redisUtils.expire(cursorKey, CURSOR_EXPIRE_MINUTES, TimeUnit.MINUTES);

        log.debug("光标更新: userId={}, documentId={}, pos=[{},{}]",
                cursor.getUserId(), cursor.getDocumentId(), cursor.getFrom(), cursor.getTo());
    }

    public List<CursorPosition> getDocumentCursors(String documentId) {
        String cursorKey = REDIS_CURSOR_PREFIX + documentId;
        Map<Object, Object> cursorMap = redisUtils.hGetAll(cursorKey);
        List<CursorPosition> cursors = new ArrayList<>();

        if (cursorMap != null) {
            for (Map.Entry<Object, Object> entry : cursorMap.entrySet()) {
                String userId = entry.getKey().toString();
                String value = entry.getValue().toString();
                String[] parts = value.split(":", 4);
                if (parts.length >= 4) {
                    CursorPosition cursor = new CursorPosition();
                    cursor.setDocumentId(documentId);
                    cursor.setUserId(userId);
                    cursor.setUserName(parts[0]);
                    cursor.setFrom(Integer.parseInt(parts[1]));
                    cursor.setTo(Integer.parseInt(parts[2]));
                    cursor.setTimestamp(Long.parseLong(parts[3]));
                    cursors.add(cursor);
                }
            }
        }
        return cursors;
    }

    public void removeCursor(String documentId, String userId) {
        String cursorKey = REDIS_CURSOR_PREFIX + documentId;
        redisUtils.hDelete(cursorKey, userId);
        log.debug("光标移除: userId={}, documentId={}", userId, documentId);
    }
}