package com.javaee.aiservice.service;

import com.javaee.aiservice.client.DocumentServiceClient;
import com.javaee.aiservice.dto.FileDeleteDTO;
import com.javaee.aiservice.dto.FileRestoreDTO;
import com.javaee.aiservice.security.BucketPermissionService;
import com.javaee.aiservice.security.RequestUserContext;
import com.javaee.aiservice.vo.FileDeleteVO;
import com.javaee.aiservice.vo.FileRestoreVO;
import com.javaee.common.utils.UserBucketUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FileDeleteService {

    private static final Logger log = LoggerFactory.getLogger(FileDeleteService.class);
    private static final String DELETE_CONFIRM_PREFIX = "file:delete:confirm:";

    @Autowired
    private RecycleBinService recycleBinService;

    @Autowired
    private MinIOService minIOService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RequestUserContext requestUserContext;

    @Autowired
    private BucketPermissionService bucketPermissionService;

    @Autowired
    private DocumentServiceClient documentServiceClient;

    @org.springframework.beans.factory.annotation.Value("${minio.delete.confirmation-timeout:300}")
    private int confirmationTimeout;

    public static class DeleteRequest implements Serializable {
        private static final long serialVersionUID = 1L;
        String documentId;
        String bucketName;
        String objectName;
        String deleter;
        boolean documentScoped;
        long createTime;
        long expiryTime;
    }

    private record DeleteTarget(String documentId, String bucketName, String objectName, boolean documentScoped) {
    }

    public FileDeleteVO deleteFile(FileDeleteDTO dto, String deleter) {
        log.info("Start deleting file: documentId={}, bucket={}, object={}, deleter={}",
                dto.getDocumentId(), dto.getBucketName(), dto.getObjectName(), deleter);

        try {
            if (dto.getConfirmationToken() != null) {
                return confirmDelete(dto);
            }

            DeleteTarget target = resolveDeleteTarget(dto);
            if (!target.documentScoped()) {
                bucketPermissionService.assertCanAccess(target.bucketName());
            }

            if (Boolean.TRUE.equals(dto.getRequireConfirmation())) {
                return requestConfirmation(target, deleter);
            }

            if (deleter == null || !deleter.startsWith("agent-approved:")) {
                log.warn("Refuse direct delete without confirmation: documentId={}, bucket={}, object={}, deleter={}",
                        target.documentId(), target.bucketName(), target.objectName(), deleter);
                return requestConfirmation(target, deleter);
            }

            return deleteDirectly(target, deleter);

        } catch (Exception e) {
            log.error("Delete file failed", e);
            throw new RuntimeException("删除文件失败: " + e.getMessage(), e);
        }
    }

    private FileDeleteVO requestConfirmation(DeleteTarget target, String deleter) {
        log.info("Request delete confirmation: documentId={}, bucket={}, object={}",
                target.documentId(), target.bucketName(), target.objectName());

        String confirmationToken = UUID.randomUUID().toString();
        long createTime = System.currentTimeMillis();
        long expiryTime = createTime + (long) confirmationTimeout * 1000;

        DeleteRequest request = new DeleteRequest();
        request.documentId = target.documentId();
        request.bucketName = target.bucketName();
        request.objectName = target.objectName();
        request.deleter = deleter;
        request.documentScoped = target.documentScoped();
        request.createTime = createTime;
        request.expiryTime = expiryTime;

        redisTemplate.opsForValue().set(DELETE_CONFIRM_PREFIX + confirmationToken, request,
                Duration.ofSeconds(confirmationTimeout));

        FileDeleteVO vo = new FileDeleteVO();
        vo.setStatus("pending");
        vo.setConfirmationToken(confirmationToken);
        vo.setConfirmationExpiry(expiryTime);
        vo.setMessage("请确认永久删除操作，token在" + confirmationTimeout + "秒内有效");

        log.info("Delete confirmation request created: token={}", confirmationToken);
        return vo;
    }

    private FileDeleteVO confirmDelete(FileDeleteDTO dto) throws Exception {
        log.info("Confirm delete: token={}", dto.getConfirmationToken());

        String confirmationToken = dto.getConfirmationToken();
        String key = DELETE_CONFIRM_PREFIX + confirmationToken;
        Object value = redisTemplate.opsForValue().get(key);
        redisTemplate.delete(key);
        DeleteRequest request = value instanceof DeleteRequest deleteRequest ? deleteRequest : null;

        if (request == null) {
            throw new IllegalArgumentException("确认token无效或已过期");
        }

        long now = System.currentTimeMillis();
        if (now > request.expiryTime) {
            throw new IllegalStateException("确认token已过期，请重新请求删除");
        }

        DeleteTarget target = new DeleteTarget(request.documentId, request.bucketName,
                request.objectName, request.documentScoped);
        if (!target.documentScoped()) {
            bucketPermissionService.assertCanAccess(target.bucketName());
        }
        return deleteDirectly(target, request.deleter);
    }

    private FileDeleteVO deleteDirectly(DeleteTarget target, String deleter) throws Exception {
        log.info("Execute direct delete: documentId={}, bucket={}, object={}, deleter={}",
                target.documentId(), target.bucketName(), target.objectName(), deleter);

        if (target.documentScoped()) {
            documentServiceClient.deleteDocument(target.documentId());
        } else {
            minIOService.deleteFile(target.bucketName(), target.objectName());
        }

        FileDeleteVO vo = new FileDeleteVO();
        vo.setStatus("deleted");
        vo.setMessage(target.documentScoped() ? "文档已永久删除" : "文件已永久删除");

        log.info("Direct delete completed: documentId={}, bucket={}, object={}",
                target.documentId(), target.bucketName(), target.objectName());
        return vo;
    }

    public FileRestoreVO restoreFile(FileRestoreDTO dto) {
        log.info("Restore file: recycleId={}", dto.getRecycleId());

        try {
            String newObjectName = recycleBinService.restoreFromRecycleBin(
                    dto.getRecycleId(), dto.getNewObjectName(), requestUserContext.getRequiredUserId());

            String bucketName = resolveBucketName(dto.getBucketName());
            bucketPermissionService.assertCanAccess(bucketName);

            FileRestoreVO vo = new FileRestoreVO();
            vo.setStatus("restored");
            vo.setBucketName(bucketName);
            vo.setObjectName(newObjectName);
            vo.setMessage("文件恢复成功");

            log.info("File restored: bucket={}, object={}", bucketName, newObjectName);
            return vo;

        } catch (Exception e) {
            log.error("Restore file failed", e);
            throw new RuntimeException("恢复文件失败: " + e.getMessage(), e);
        }
    }

    public void cleanupExpiredConfirmations() {
        log.info("Start cleaning expired delete confirmations");

        try {
            long now = System.currentTimeMillis();
            int removedCount = 0;

            for (String key : scanConfirmKeys()) {
                DeleteRequest request = (DeleteRequest) redisTemplate.opsForValue().get(key);
                if (request != null && now > request.expiryTime) {
                    redisTemplate.delete(key);
                    removedCount++;
                }
            }

            log.info("Expired delete confirmations cleaned: removed={}", removedCount);

        } catch (Exception e) {
            log.error("Clean expired delete confirmations failed", e);
        }
    }

    private List<String> scanConfirmKeys() {
        List<String> keys = new ArrayList<>();
        ScanOptions options = ScanOptions.scanOptions().match(DELETE_CONFIRM_PREFIX + "*").count(200).build();
        try (var cursor = redisTemplate.getConnectionFactory().getConnection().scan(options)) {
            while (cursor.hasNext()) {
                keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
            }
        }
        return keys;
    }

    private String resolveBucketName(String requestedBucketName) {
        String bucketName = trimToNull(requestedBucketName);
        if (bucketName != null) {
            return bucketName;
        }
        return UserBucketUtils.bucketNameForUser(requestUserContext.getRequiredUserId());
    }

    private DeleteTarget resolveDeleteTarget(FileDeleteDTO dto) {
        String documentId = trimToNull(dto.getDocumentId());
        if (documentId != null) {
            return new DeleteTarget(documentId, null, null, true);
        }

        String bucketName = resolveBucketName(dto.getBucketName());
        String objectName = trimToNull(dto.getObjectName());
        if (objectName == null) {
            throw new IllegalArgumentException("对象名称不能为空，或提供前端documentId");
        }
        return new DeleteTarget(null, bucketName, objectName, false);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
