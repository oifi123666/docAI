package com.javaee.aiservice.service;

import com.javaee.aiservice.dto.FileDownloadDTO;
import com.javaee.aiservice.security.BucketPermissionService;
import com.javaee.aiservice.security.RequestUserContext;
import com.javaee.aiservice.vo.FileDownloadVO;
import com.javaee.common.utils.UserBucketUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * 文件下载服务
 * 功能说明：使用skill实现从minIO下载文件到本地
 */
@Service
public class FileDownloadService {

    private static final Logger log = LoggerFactory.getLogger(FileDownloadService.class);

    @Autowired
    private MinIOService minIOService;

    @Autowired
    private BucketPermissionService bucketPermissionService;

    @Autowired
    private RequestUserContext requestUserContext;

    @Value("${minio.url.expiry:3600}")
    private int defaultExpiry;

    /**
     * 下载文件功能
     * 功能说明：从minIO服务器下载文件
     * 实现方式：使用skill方式实现，通过MinIOService接口调用minIO
     * 
     * @param dto 下载参数
     * @return 文件输入流
     */
    public InputStream downloadFile(FileDownloadDTO dto) {
        log.info("开始下载文件: bucket={}, object={}", dto.getBucketName(), dto.getObjectName());

        try {
            // 确定存储桶名称
            String bucketName = resolveBucketName(dto.getBucketName());
            bucketPermissionService.assertCanAccess(bucketName);
            
            // 确定对象名称
            String objectName = dto.getObjectName();
            if (objectName == null || objectName.trim().isEmpty()) {
                throw new IllegalArgumentException("对象名称不能为空");
            }

            // 调用MinIOService下载文件
            log.info("调用MinIO下载文件: bucket={}, object={}", bucketName, objectName);
            InputStream inputStream = minIOService.downloadFile(bucketName, objectName);

            log.info("文件下载成功: bucket={}, object={}", bucketName, objectName);
            return inputStream;

        } catch (Exception e) {
            log.error("文件下载失败", e);
            throw new RuntimeException("文件下载失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取文件访问URL
     * 功能说明：获取minIO中文件的预签名访问URL
     * 实现方式：使用skill方式实现，通过MinIOService接口调用minIO
     * 
     * @param dto 下载参数
     * @return 文件下载结果
     */
    public FileDownloadVO getFileUrl(FileDownloadDTO dto) {
        log.info("开始获取文件URL: bucket={}, object={}", dto.getBucketName(), dto.getObjectName());

        try {
            // 确定存储桶名称
            String bucketName = resolveBucketName(dto.getBucketName());
            bucketPermissionService.assertCanAccess(bucketName);
            
            // 确定对象名称
            String objectName = dto.getObjectName();
            if (objectName == null || objectName.trim().isEmpty()) {
                throw new IllegalArgumentException("对象名称不能为空");
            }

            // 确定过期时间
            int expirySeconds = defaultExpiry;

            // 调用MinIOService获取文件URL
            log.info("调用MinIO获取文件URL: bucket={}, object={}, expiry={}s", 
                bucketName, objectName, expirySeconds);
            String fileUrl = minIOService.getFileUrl(bucketName, objectName, expirySeconds);

            // 构建返回结果
            FileDownloadVO vo = new FileDownloadVO();
            vo.setFileUrl(fileUrl);
            vo.setBucketName(bucketName);
            vo.setObjectName(objectName);
            vo.setExpirySeconds(expirySeconds);

            log.info("获取文件URL成功: url={}", fileUrl);
            return vo;

        } catch (Exception e) {
            log.error("获取文件URL失败", e);
            throw new RuntimeException("获取文件URL失败: " + e.getMessage(), e);
        }
    }

    private String resolveBucketName(String requestedBucketName) {
        if (requestedBucketName != null && !requestedBucketName.isBlank()) {
            return requestedBucketName;
        }
        return UserBucketUtils.bucketNameForUser(requestUserContext.getRequiredUserId());
    }
}
