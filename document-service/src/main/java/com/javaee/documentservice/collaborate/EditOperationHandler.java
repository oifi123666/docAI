package com.javaee.documentservice.collaborate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaee.common.utils.RedisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class EditOperationHandler {

    private static final Logger log = LoggerFactory.getLogger(EditOperationHandler.class);

    private static final String REDIS_OPS_PREFIX = "collaborate:ops:";
    private static final String REDIS_CLOCK_PREFIX = "collaborate:clock:";
    private static final int MAX_CACHED_OPS = 500;
    private static final long OPS_EXPIRE_HOURS = 24;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public EditOperation transformAndApply(EditOperation remoteOp, String localUserId) {
        List<EditOperation> recentOps = getRecentOperations(remoteOp.getDocumentId());

        EditOperation transformedOp = remoteOp;
        for (EditOperation localOp : recentOps) {
            if (localOp.getUserId().equals(localUserId) && localOp.getClock() > remoteOp.getClock()) {
                transformedOp = transform(transformedOp, localOp);
            }
        }

        saveOperation(transformedOp);
        return transformedOp;
    }

    public EditOperation transform(EditOperation remoteOp, EditOperation localOp) {
        if (remoteOp.getType() == EditOperation.OpType.INSERT && localOp.getType() == EditOperation.OpType.INSERT) {
            if (localOp.getFrom() <= remoteOp.getFrom()) {
                EditOperation transformed = new EditOperation();
                transformed.setOpId(remoteOp.getOpId());
                transformed.setDocumentId(remoteOp.getDocumentId());
                transformed.setUserId(remoteOp.getUserId());
                transformed.setUserName(remoteOp.getUserName());
                transformed.setType(EditOperation.OpType.INSERT);
                transformed.setFrom(remoteOp.getFrom() + localOp.getText().length());
                transformed.setTo(remoteOp.getTo() + localOp.getText().length());
                transformed.setText(remoteOp.getText());
                transformed.setClock(remoteOp.getClock());
                return transformed;
            }
        }

        if (remoteOp.getType() == EditOperation.OpType.DELETE && localOp.getType() == EditOperation.OpType.INSERT) {
            if (localOp.getFrom() <= remoteOp.getFrom()) {
                EditOperation transformed = new EditOperation();
                transformed.setOpId(remoteOp.getOpId());
                transformed.setDocumentId(remoteOp.getDocumentId());
                transformed.setUserId(remoteOp.getUserId());
                transformed.setUserName(remoteOp.getUserName());
                transformed.setType(EditOperation.OpType.DELETE);
                transformed.setFrom(remoteOp.getFrom() + localOp.getText().length());
                transformed.setTo(remoteOp.getTo() + localOp.getText().length());
                transformed.setText("");
                transformed.setClock(remoteOp.getClock());
                return transformed;
            }
        }

        return remoteOp;
    }

    public void saveOperation(EditOperation operation) {
        String opsKey = REDIS_OPS_PREFIX + operation.getDocumentId();
        try {
            String opJson = objectMapper.writeValueAsString(operation);
            redisUtils.lRightPush(opsKey, opJson);
            redisUtils.expire(opsKey, OPS_EXPIRE_HOURS, TimeUnit.HOURS);

            Long size = redisUtils.lSize(opsKey);
            if (size != null && size > MAX_CACHED_OPS) {
                redisUtils.lLeftPop(opsKey);
            }
        } catch (JsonProcessingException e) {
            log.error("序列化编辑操作失败: {}", operation.getOpId(), e);
        }
    }

    public List<EditOperation> getRecentOperations(String documentId) {
        String opsKey = REDIS_OPS_PREFIX + documentId;
        List<Object> rawList = redisUtils.lRange(opsKey, 0, MAX_CACHED_OPS);
        List<EditOperation> operations = new ArrayList<>();

        if (rawList != null) {
            for (Object obj : rawList) {
                try {
                    EditOperation op = objectMapper.readValue(obj.toString(), EditOperation.class);
                    operations.add(op);
                } catch (JsonProcessingException e) {
                    log.warn("反序列化编辑操作失败: {}", obj, e);
                }
            }
        }
        return operations;
    }

    public long getNextClock(String documentId, String userId) {
        String clockKey = REDIS_CLOCK_PREFIX + documentId + ":" + userId;
        Long value = redisTemplate.opsForValue().increment(clockKey, 1);
        redisUtils.expire(clockKey, OPS_EXPIRE_HOURS, TimeUnit.HOURS);
        return value != null ? value : 1;
    }

    public List<EditOperation> getOperationsSince(String documentId, long sinceClock) {
        List<EditOperation> allOps = getRecentOperations(documentId);
        List<EditOperation> result = new ArrayList<>();
        for (EditOperation op : allOps) {
            if (op.getClock() > sinceClock) {
                result.add(op);
            }
        }
        return result;
    }
}