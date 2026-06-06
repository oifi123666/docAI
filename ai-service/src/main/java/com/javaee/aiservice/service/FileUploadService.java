package com.javaee.aiservice.service;

import com.javaee.aiservice.dto.FileUploadDTO;
import com.javaee.aiservice.security.BucketPermissionService;
import com.javaee.aiservice.security.RequestUserContext;
import com.javaee.aiservice.vo.FileUploadVO;
import com.javaee.common.utils.UserBucketUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 文件上传服务
 * 功能说明：使用skill实现文件上传至minIO
 */
@Service
public class FileUploadService {

    private static final Logger log = LoggerFactory.getLogger(FileUploadService.class);

    @Autowired
    private MinIOService minIOService;

    @Autowired
    private FileVersionService fileVersionService;

    @Autowired
    private RequestUserContext requestUserContext;

    @Autowired
    private BucketPermissionService bucketPermissionService;

    /**
     * 文件上传功能
     * 功能说明：将本地文件上传至minIO服务器
     * 实现方式：使用skill方式实现，通过MinIOService接口调用minIO
     * 
     * @param file 上传的文件
     * @param dto 上传参数
     * @return 文件上传结果
     */
    public FileUploadVO uploadFile(MultipartFile file, FileUploadDTO dto) {
        log.info("开始上传文件: filename={}", file.getOriginalFilename());

        try {
            // 确定存储桶名称
            String bucketName = resolveBucketName(dto.getBucketName());
            bucketPermissionService.assertCanAccess(bucketName);
            
            // 确定对象名称（文件路径）
            String objectName = dto.getObjectName() != null ? dto.getObjectName() : generateObjectName(file);

            // 调用MinIOService上传文件
            log.info("调用MinIO上传文件: bucket={}, object={}", bucketName, objectName);
            String fileUrl = minIOService.uploadFile(file, bucketName, objectName);

            // 创建文件版本记录
            log.info("创建文件版本记录: bucket={}, object={}", bucketName, objectName);
            String versionId = fileVersionService.createVersion(
                bucketName, objectName, requestUserContext.getRequiredUserId(), file.getSize(), "文件上传");

            // 构建返回结果
            FileUploadVO vo = new FileUploadVO();
            vo.setFileUrl(fileUrl);
            vo.setBucketName(bucketName);
            vo.setObjectName(objectName);
            vo.setOriginalFilename(file.getOriginalFilename());
            vo.setFileSize(file.getSize());
            vo.setContentType(file.getContentType());
            vo.setUploadTime(System.currentTimeMillis());

            log.info("文件上传成功: url={}, versionId={}", fileUrl, versionId);
            return vo;

        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成对象名称（文件路径）
     * 格式：yyyy/MM/dd/UUID_原文件名
     * 
     * @param file 上传的文件
     * @return 生成的对象名称
     */
    private String generateObjectName(MultipartFile file) {
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String originalFilename = file.getOriginalFilename();
        
        // 获取文件扩展名
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        return datePath + "/" + uuid + extension;
    }

    private String resolveBucketName(String requestedBucketName) {
        if (requestedBucketName != null && !requestedBucketName.isBlank()) {
            return requestedBucketName;
        }
        return UserBucketUtils.bucketNameForUser(requestUserContext.getRequiredUserId());
    }
}
