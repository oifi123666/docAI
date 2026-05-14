package com.javaee.aiservice.service;

import com.javaee.aiservice.dto.FileDeleteDTO;
import com.javaee.aiservice.dto.FileRestoreDTO;
import com.javaee.aiservice.security.RequestUserContext;
import com.javaee.aiservice.vo.FileDeleteVO;
import com.javaee.aiservice.vo.FileRestoreVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 文件删除服务
 * 功能说明：使用MCP实现文件删除功能，支持确认删除和回收站
 * 实现方式：使用skill方式实现，等待MCP集成
 */
@Service
public class FileDeleteService {

    private static final Logger log = LoggerFactory.getLogger(FileDeleteService.class);
    private static final String DELETE_CONFIRM_PREFIX = "file:delete:confirm:";

    @Autowired
    private MinIOService minIOService;

    @Autowired
    private RecycleBinService recycleBinService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RequestUserContext requestUserContext;

    @Value("${minio.bucket:documents}")
    private String defaultBucket;

    @Value("${minio.delete.confirmation-timeout:300}")
    private int confirmationTimeout;

    /**
     * 删除请求
     */
    public static class DeleteRequest implements Serializable {
        private static final long serialVersionUID = 1L;
        String bucketName;
        String objectName;
        String deleter;
        long createTime;
        long expiryTime;
    }

    /**
     * 删除文件（使用MCP）
     * 功能说明：删除文件，支持确认删除和回收站
     * 实现方式：使用skill方式实现，通过MCP进行权限验证
     * 
     * @param dto 删除参数
     * @param deleter 删除者
     * @return 删除结果
     */
    public FileDeleteVO deleteFile(FileDeleteDTO dto, String deleter) {
        log.info("开始删除文件: bucket={}, object={}, deleter={}", 
            dto.getBucketName(), dto.getObjectName(), deleter);

        try {
            String bucketName = dto.getBucketName() != null ? dto.getBucketName() : defaultBucket;
            String objectName = dto.getObjectName();

            if (objectName == null || objectName.trim().isEmpty()) {
                throw new IllegalArgumentException("对象名称不能为空");
            }

            if (dto.getConfirmationToken() != null) {
                return confirmDelete(dto);
            }

            if (Boolean.TRUE.equals(dto.getRequireConfirmation())) {
                return requestConfirmation(bucketName, objectName, deleter);
            }

            if (deleter == null || !deleter.startsWith("agent-approved:")) {
                log.warn("拒绝未确认的直接删除请求: bucket={}, object={}, deleter={}", bucketName, objectName, deleter);
                return requestConfirmation(bucketName, objectName, deleter);
            }

            return deleteWithRecycle(bucketName, objectName, deleter);

        } catch (Exception e) {
            log.error("删除文件失败", e);
            throw new RuntimeException("删除文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 请求确认删除
     */
    private FileDeleteVO requestConfirmation(String bucketName, String objectName, String deleter) {
        log.info("请求确认删除: bucket={}, object={}", bucketName, objectName);

        String confirmationToken = UUID.randomUUID().toString();
        long createTime = System.currentTimeMillis();
        long expiryTime = createTime + (long) confirmationTimeout * 1000;

        DeleteRequest request = new DeleteRequest();
        request.bucketName = bucketName;
        request.objectName = objectName;
        request.deleter = deleter;
        request.createTime = createTime;
        request.expiryTime = expiryTime;

        redisTemplate.opsForValue().set(DELETE_CONFIRM_PREFIX + confirmationToken, request,
                Duration.ofSeconds(confirmationTimeout));

        FileDeleteVO vo = new FileDeleteVO();
        vo.setStatus("pending");
        vo.setConfirmationToken(confirmationToken);
        vo.setConfirmationExpiry(expiryTime);
        vo.setMessage("请确认删除操作，token在" + confirmationTimeout + "秒内有效");

        log.info("删除确认请求已发送: token={}", confirmationToken);
        return vo;
    }

    /**
     * 确认删除
     */
    private FileDeleteVO confirmDelete(FileDeleteDTO dto) {
        log.info("确认删除: token={}", dto.getConfirmationToken());

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

        return deleteWithRecycle(request.bucketName, request.objectName, request.deleter);
    }

    /**
     * 带回收站的删除
     */
    private FileDeleteVO deleteWithRecycle(String bucketName, String objectName, String deleter) {
        log.info("执行删除（带回收站）: bucket={}, object={}", bucketName, objectName);

        String recycleId = recycleBinService.moveToRecycleBin(bucketName, objectName, deleter);

        FileDeleteVO vo = new FileDeleteVO();
        vo.setStatus("recycle");
        vo.setRecycleId(recycleId);
        vo.setMessage("文件已移至回收站，可以在有效期内恢复");

        log.info("文件已移至回收站: recycleId={}", recycleId);
        return vo;
    }

    /**
     * 恢复文件
     * 
     * @param dto 恢复参数
     * @return 恢复结果
     */
    public FileRestoreVO restoreFile(FileRestoreDTO dto) {
        log.info("恢复文件: recycleId={}", dto.getRecycleId());

        try {
            String newObjectName = recycleBinService.restoreFromRecycleBin(
                dto.getRecycleId(), dto.getNewObjectName(), requestUserContext.getRequiredUserId());

            String bucketName = dto.getBucketName() != null ? dto.getBucketName() : defaultBucket;

            FileRestoreVO vo = new FileRestoreVO();
            vo.setStatus("restored");
            vo.setBucketName(bucketName);
            vo.setObjectName(newObjectName);
            vo.setMessage("文件恢复成功");

            log.info("文件恢复成功: bucket={}, object={}", bucketName, newObjectName);
            return vo;

        } catch (Exception e) {
            log.error("恢复文件失败", e);
            throw new RuntimeException("恢复文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 清理过期的确认请求
     * 【待实现】定期调用此方法清理过期的确认请求
     */
    public void cleanupExpiredConfirmations() {
        log.info("开始清理过期的删除确认请求");

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

            log.info("清理完成，删除了 {} 个过期确认请求", removedCount);

        } catch (Exception e) {
            log.error("清理过期确认请求失败", e);
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
}
