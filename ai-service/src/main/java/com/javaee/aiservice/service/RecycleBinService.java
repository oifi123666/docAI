package com.javaee.aiservice.service;

import com.javaee.aiservice.vo.RecycleBinVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 回收站服务
 * 功能说明：管理已删除的文件，支持在有效期内恢复
 * 实现方式：Redis持久化记录 + MinIO回收站对象路径
 */
@Service
public class RecycleBinService {

    private static final Logger log = LoggerFactory.getLogger(RecycleBinService.class);
    private static final String RECYCLE_PREFIX = "recycle:file:";
    private static final String RECYCLE_OBJECT_PREFIX = ".recycle/";

    @Autowired
    private MinIOService minIOService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${minio.bucket:documents}")
    private String defaultBucket;

    @Value("${minio.recycle.expiry-days:7}")
    private int recycleExpiryDays;

    /**
     * 将文件移动到回收站
     * 
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @param deleter 删除者
     * @return 回收站记录ID
     */
    public String moveToRecycleBin(String bucketName, String objectName, String deleter) {
        log.info("将文件移动到回收站: bucket={}, object={}", bucketName, objectName);

        try {
            String recycleId = UUID.randomUUID().toString();
            long deleteTime = System.currentTimeMillis();
            long expiryTime = deleteTime + (long) recycleExpiryDays * 24 * 60 * 60 * 1000;

            RecycleBinVO.RecycleFile recycleFile = new RecycleBinVO.RecycleFile();
            recycleFile.setRecycleId(recycleId);
            recycleFile.setBucketName(bucketName);
            recycleFile.setOriginalObjectName(objectName);
            recycleFile.setDeleteTime(deleteTime);
            recycleFile.setExpiryTime(expiryTime);
            recycleFile.setFileSize(0L);
            recycleFile.setDeleter(deleter);

            String recycleObjectName = recycleObjectName(recycleId, objectName);
            try (InputStream inputStream = minIOService.downloadFile(bucketName, objectName)) {
                byte[] bytes = inputStream.readAllBytes();
                minIOService.uploadBytes(bytes, bucketName, recycleObjectName, "application/octet-stream");
                minIOService.deleteFile(bucketName, objectName);
                recycleFile.setFileSize((long) bytes.length);
            }

            redisTemplate.opsForValue().set(RECYCLE_PREFIX + recycleId, recycleFile,
                    Duration.ofDays(recycleExpiryDays));

            log.info("文件已移动到回收站: recycleId={}", recycleId);
            return recycleId;

        } catch (Exception e) {
            log.error("移动文件到回收站失败", e);
            throw new RuntimeException("移动文件到回收站失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从回收站恢复文件
     * 
     * @param recycleId 回收站记录ID
     * @param newObjectName 新对象名称（可选）
     * @return 恢复后的对象名称
     */
    public String restoreFromRecycleBin(String recycleId, String newObjectName) {
        return restoreFromRecycleBin(recycleId, newObjectName, null);
    }

    public String restoreFromRecycleBin(String recycleId, String newObjectName, String requester) {
        log.info("从回收站恢复文件: recycleId={}", recycleId);

        try {
            RecycleBinVO.RecycleFile recycleFile = getRecycleFile(recycleId);
            if (recycleFile == null) {
                throw new IllegalArgumentException("回收站记录不存在: " + recycleId);
            }

            long now = System.currentTimeMillis();
            if (now > recycleFile.getExpiryTime()) {
                throw new IllegalStateException("文件已超过回收站保留期，无法恢复");
            }
            if (requester != null && !requester.equals(normalizeDeleter(recycleFile.getDeleter()))) {
                throw new SecurityException("无权恢复该回收站文件");
            }

            String targetObjectName = newObjectName != null ? newObjectName : recycleFile.getOriginalObjectName();
            String recycleObjectName = recycleObjectName(recycleId, recycleFile.getOriginalObjectName());

            log.info("从回收站恢复文件: original={}, target={}", 
                recycleFile.getOriginalObjectName(), targetObjectName);

            try (InputStream inputStream = minIOService.downloadFile(recycleFile.getBucketName(), recycleObjectName)) {
                byte[] bytes = inputStream.readAllBytes();
                minIOService.uploadBytes(bytes, recycleFile.getBucketName(), targetObjectName, "application/octet-stream");
                minIOService.deleteFile(recycleFile.getBucketName(), recycleObjectName);
            }

            redisTemplate.delete(RECYCLE_PREFIX + recycleId);

            return targetObjectName;

        } catch (Exception e) {
            log.error("从回收站恢复文件失败", e);
            throw new RuntimeException("从回收站恢复文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取回收站文件列表
     * 
     * @param bucketName 存储桶名称（可选）
     * @return 回收站文件列表
     */
    public RecycleBinVO listRecycleBin(String bucketName) {
        return listRecycleBin(bucketName, null);
    }

    public RecycleBinVO listRecycleBin(String bucketName, String deleter) {
        log.info("获取回收站文件列表: bucket={}", bucketName);

        try {
            List<RecycleBinVO.RecycleFile> files = new ArrayList<>();
            long now = System.currentTimeMillis();

            for (String key : scanKeys(RECYCLE_PREFIX + "*")) {
                RecycleBinVO.RecycleFile file = (RecycleBinVO.RecycleFile) redisTemplate.opsForValue().get(key);
                if (file == null) {
                    continue;
                }
                if (now > file.getExpiryTime()) {
                    continue;
                }
                if (bucketName != null && !bucketName.equals(file.getBucketName())) {
                    continue;
                }
                if (deleter != null && !deleter.equals(normalizeDeleter(file.getDeleter()))) {
                    continue;
                }
                files.add(file);
            }

            RecycleBinVO vo = new RecycleBinVO();
            vo.setTotalCount(files.size());
            vo.setFiles(files);

            log.info("回收站文件列表: count={}", files.size());
            return vo;

        } catch (Exception e) {
            log.error("获取回收站文件列表失败", e);
            throw new RuntimeException("获取回收站文件列表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 永久删除回收站中的文件
     * 
     * @param recycleId 回收站记录ID
     */
    public void permanentDelete(String recycleId) {
        log.info("永久删除回收站中的文件: recycleId={}", recycleId);

        try {
            RecycleBinVO.RecycleFile recycleFile = getRecycleFile(recycleId);
            if (recycleFile == null) {
                throw new IllegalArgumentException("回收站记录不存在: " + recycleId);
            }
            minIOService.deleteFile(recycleFile.getBucketName(), recycleObjectName(recycleId, recycleFile.getOriginalObjectName()));
            redisTemplate.delete(RECYCLE_PREFIX + recycleId);

            log.info("文件已永久删除: bucket={}, object={}", 
                recycleFile.getBucketName(), recycleFile.getOriginalObjectName());

        } catch (Exception e) {
            log.error("永久删除文件失败", e);
            throw new RuntimeException("永久删除文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 清理过期的回收站文件
     * 【待实现】定期调用此方法清理过期文件
     */
    public void cleanupExpiredFiles() {
        log.info("开始清理过期的回收站文件");

        try {
            long now = System.currentTimeMillis();
            int removedCount = 0;

            for (String key : scanKeys(RECYCLE_PREFIX + "*")) {
                String recycleId = key.substring(RECYCLE_PREFIX.length());
                RecycleBinVO.RecycleFile file = (RecycleBinVO.RecycleFile) redisTemplate.opsForValue().get(key);
                if (file != null && now > file.getExpiryTime()) {
                    permanentDelete(recycleId);
                    removedCount++;
                }
            }

            log.info("清理完成，删除了 {} 个过期文件", removedCount);

        } catch (Exception e) {
            log.error("清理过期文件失败", e);
        }
    }

    private RecycleBinVO.RecycleFile getRecycleFile(String recycleId) {
        Object value = redisTemplate.opsForValue().get(RECYCLE_PREFIX + recycleId);
        if (value instanceof RecycleBinVO.RecycleFile file) {
            return file;
        }
        return null;
    }

    private String recycleObjectName(String recycleId, String originalObjectName) {
        String filename = originalObjectName == null ? "unknown" : originalObjectName;
        if (filename.contains("/")) {
            filename = filename.substring(filename.lastIndexOf("/") + 1);
        }
        return RECYCLE_OBJECT_PREFIX + recycleId + "/" + filename;
    }

    private String normalizeDeleter(String deleter) {
        if (deleter == null) {
            return "";
        }
        return deleter.startsWith("agent-approved:") ? deleter.substring("agent-approved:".length()) : deleter;
    }

    private List<String> scanKeys(String pattern) {
        List<String> keys = new ArrayList<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(200).build();
        try (var cursor = redisTemplate.getConnectionFactory().getConnection().scan(options)) {
            while (cursor.hasNext()) {
                keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
            }
        }
        return keys;
    }
}
