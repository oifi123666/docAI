package com.javaee.documentservice.service.impl;

import com.javaee.common.exception.BusinessException;
import com.javaee.documentservice.security.BucketPermissionService;
import com.javaee.documentservice.service.DocumentContentService;
import io.minio.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 文档内容服务实现类
 * 将文档内容存储到MinIO
 */
@Service
public class DocumentContentServiceImpl implements DocumentContentService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentContentServiceImpl.class);

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket-name:document}")
    private String bucketName;

    @Autowired
    private BucketPermissionService bucketPermissionService;

    /**
     * 获取文档内容在MinIO中的存储键
     */
    private String getContentKey(String documentId) {
        return "document-content/" + documentId + ".txt";
    }

    @Override
    public boolean saveContent(String documentId, String content) {
        try {
            bucketPermissionService.assertCanAccess(bucketName);
            ensureBucketExists();
            String contentKey = getContentKey(documentId);
            byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
            
            logger.info("Saving document content to MinIO, bucket: {}, key: {}, size: {} bytes", 
                    bucketName, contentKey, contentBytes.length);
            
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(contentKey)
                            .stream(new java.io.ByteArrayInputStream(contentBytes), contentBytes.length, -1)
                            .contentType("text/plain; charset=UTF-8")
                            .build()
            );
            logger.info("Document content saved successfully, documentId: {}", documentId);
            return true;
        } catch (Exception e) {
            logger.error("Failed to save document content to MinIO, documentId: {}", documentId, e);
            throw new BusinessException("文档内容保存失败: " + e.getMessage());
        }
    }

    @Override
    public String getContent(String documentId) {
        try {
            bucketPermissionService.assertCanAccess(bucketName);
            String contentKey = getContentKey(documentId);
            logger.info("Getting document content from MinIO, bucket: {}, key: {}", bucketName, contentKey);
            
            try (InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(contentKey)
                            .build()
            )) {
                String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                logger.info("Document content retrieved successfully, documentId: {}, size: {} bytes", 
                        documentId, content.length());
                return content;
            }
        } catch (Exception e) {
            logger.warn("Failed to get document content from MinIO, documentId: {}, error: {}", documentId, e.getMessage());
            return null;
        }
    }

    @Override
    public boolean deleteContent(String documentId) {
        try {
            bucketPermissionService.assertCanAccess(bucketName);
            String contentKey = getContentKey(documentId);
            logger.info("Deleting document content from MinIO, bucket: {}, key: {}", bucketName, contentKey);
            
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(contentKey)
                            .build()
            );
            logger.info("Document content deleted successfully, documentId: {}", documentId);
            return true;
        } catch (Exception e) {
            logger.error("Failed to delete document content from MinIO, documentId: {}", documentId, e);
            throw new BusinessException("文档内容删除失败: " + e.getMessage());
        }
    }

    @Override
    public boolean updateContent(String documentId, String content) {
        return saveContent(documentId, content);
    }

    /**
     * 确保存储桶存在
     */
    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!exists) {
            logger.info("Bucket {} does not exist, creating...", bucketName);
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            logger.info("Bucket {} created successfully", bucketName);
        }
    }
}
