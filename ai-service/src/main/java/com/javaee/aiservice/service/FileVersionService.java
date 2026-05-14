package com.javaee.aiservice.service;

import com.javaee.aiservice.dto.FileVersionDTO;
import com.javaee.aiservice.dto.FileVersionSwitchDTO;
import com.javaee.aiservice.vo.FileVersionVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 文件版本管理服务
 * 功能说明：管理文件的多个版本，支持查看和切换
 * 实现方式：Redis持久化版本元数据 + MinIO保存版本快照
 */
@Service
public class FileVersionService {

    private static final Logger log = LoggerFactory.getLogger(FileVersionService.class);
    private static final String VERSION_PREFIX = "file:versions:";
    private static final String VERSION_OBJECT_PREFIX = ".versions/";

    @Autowired
    private MinIOService minIOService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${minio.bucket:documents}")
    private String defaultBucket;

    /**
     * 创建新的文件版本
     * 
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @param uploader 上传者
     * @param fileSize 文件大小
     * @param remark 版本备注
     * @return 新版本号
     */
    public String createVersion(String bucketName, String objectName, String uploader, Long fileSize, String remark) {
        log.info("创建文件版本: bucket={}, object={}", bucketName, objectName);

        try {
            String actualBucket = bucketName != null ? bucketName : defaultBucket;
            String key = versionKey(actualBucket, objectName);
            String versionId = UUID.randomUUID().toString();
            long createTime = System.currentTimeMillis();

            FileVersionVO.VersionInfo versionInfo = new FileVersionVO.VersionInfo();
            versionInfo.setVersionId(versionId);
            versionInfo.setIsCurrent(true);
            versionInfo.setCreateTime(createTime);
            versionInfo.setFileSize(fileSize);
            versionInfo.setUploader(uploader);
            versionInfo.setRemark(remark);

            String versionObjectName = versionObjectName(objectName, versionId);
            try (InputStream inputStream = minIOService.downloadFile(actualBucket, objectName)) {
                byte[] bytes = inputStream.readAllBytes();
                minIOService.uploadBytes(bytes, actualBucket, versionObjectName, "application/octet-stream");
                versionInfo.setFileSize((long) bytes.length);
            }

            List<FileVersionVO.VersionInfo> versions = loadVersions(key);
            
            for (FileVersionVO.VersionInfo version : versions) {
                version.setIsCurrent(false);
            }
            
            versions.add(0, versionInfo);
            saveVersions(key, versions);

            log.info("文件版本创建成功: versionId={}", versionId);
            return versionId;

        } catch (Exception e) {
            log.error("创建文件版本失败", e);
            throw new RuntimeException("创建文件版本失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取文件版本列表
     * 
     * @param dto 查询参数
     * @return 文件版本信息
     */
    public FileVersionVO getVersions(FileVersionDTO dto) {
        log.info("获取文件版本列表: bucket={}, object={}", dto.getBucketName(), dto.getObjectName());

        try {
            String bucketName = dto.getBucketName() != null ? dto.getBucketName() : defaultBucket;
            String objectName = dto.getObjectName();
            String key = versionKey(bucketName, objectName);

            List<FileVersionVO.VersionInfo> versions = loadVersions(key);
            String currentVersionId = null;
            
            for (FileVersionVO.VersionInfo version : versions) {
                if (version.getIsCurrent()) {
                    currentVersionId = version.getVersionId();
                    break;
                }
            }

            FileVersionVO vo = new FileVersionVO();
            vo.setBucketName(bucketName);
            vo.setObjectName(objectName);
            vo.setCurrentVersionId(currentVersionId);
            vo.setVersions(versions);

            log.info("文件版本列表: count={}", versions.size());
            return vo;

        } catch (Exception e) {
            log.error("获取文件版本列表失败", e);
            throw new RuntimeException("获取文件版本列表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 切换文件版本
     * 
     * @param dto 切换参数
     * @return 切换后的版本信息
     */
    public FileVersionVO.VersionInfo switchVersion(FileVersionSwitchDTO dto) {
        log.info("切换文件版本: bucket={}, object={}, targetVersion={}", 
            dto.getBucketName(), dto.getObjectName(), dto.getTargetVersionId());

        try {
            String bucketName = dto.getBucketName() != null ? dto.getBucketName() : defaultBucket;
            String objectName = dto.getObjectName();
            String key = versionKey(bucketName, objectName);
            String targetVersionId = dto.getTargetVersionId();

            List<FileVersionVO.VersionInfo> versions = loadVersions(key);
            if (versions.isEmpty()) {
                throw new IllegalArgumentException("文件版本不存在: " + objectName);
            }

            FileVersionVO.VersionInfo targetVersion = null;
            for (FileVersionVO.VersionInfo version : versions) {
                if (version.getVersionId().equals(targetVersionId)) {
                    targetVersion = version;
                }
                version.setIsCurrent(false);
            }

            if (targetVersion == null) {
                throw new IllegalArgumentException("目标版本不存在: " + targetVersionId);
            }

            String versionObjectName = versionObjectName(objectName, targetVersionId);
            try (InputStream inputStream = minIOService.downloadFile(bucketName, versionObjectName)) {
                byte[] bytes = inputStream.readAllBytes();
                minIOService.uploadBytes(bytes, bucketName, objectName, "application/octet-stream");
            }

            targetVersion.setIsCurrent(true);
            saveVersions(key, versions);
            
            log.info("文件版本切换成功: versionId={}", targetVersionId);
            return targetVersion;

        } catch (Exception e) {
            log.error("切换文件版本失败", e);
            throw new RuntimeException("切换文件版本失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取指定版本的文件信息
     * 
     * @param dto 查询参数
     * @return 版本信息
     */
    public FileVersionVO.VersionInfo getVersionInfo(FileVersionDTO dto) {
        log.info("获取文件版本信息: bucket={}, object={}, version={}", 
            dto.getBucketName(), dto.getObjectName(), dto.getVersionId());

        try {
            String bucketName = dto.getBucketName() != null ? dto.getBucketName() : defaultBucket;
            String objectName = dto.getObjectName();
            String versionId = dto.getVersionId();
            String key = versionKey(bucketName, objectName);

            List<FileVersionVO.VersionInfo> versions = loadVersions(key);
            if (versions.isEmpty()) {
                throw new IllegalArgumentException("文件版本不存在: " + objectName);
            }

            for (FileVersionVO.VersionInfo version : versions) {
                if (version.getVersionId().equals(versionId)) {
                    return version;
                }
            }

            throw new IllegalArgumentException("版本不存在: " + versionId);

        } catch (Exception e) {
            log.error("获取文件版本信息失败", e);
            throw new RuntimeException("获取文件版本信息失败: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<FileVersionVO.VersionInfo> loadVersions(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof List<?> list) {
            return new ArrayList<>((List<FileVersionVO.VersionInfo>) list);
        }
        return new ArrayList<>();
    }

    private void saveVersions(String key, List<FileVersionVO.VersionInfo> versions) {
        redisTemplate.opsForValue().set(key, versions);
    }

    private String versionKey(String bucketName, String objectName) {
        return VERSION_PREFIX + bucketName + ":" + objectName;
    }

    private String versionObjectName(String objectName, String versionId) {
        String safeObjectName = objectName == null ? "unknown" : objectName.replaceAll("^/+", "");
        return VERSION_OBJECT_PREFIX + safeObjectName + "/" + versionId;
    }
}
