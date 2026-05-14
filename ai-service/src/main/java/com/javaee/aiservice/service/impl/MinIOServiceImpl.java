package com.javaee.aiservice.service.impl;

import com.javaee.aiservice.service.MinIOService;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * MinIO服务实现类
 * 功能说明：提供文件上传、下载、删除等操作
 */
@Service
public class MinIOServiceImpl implements MinIOService {

    private static final Logger log = LoggerFactory.getLogger(MinIOServiceImpl.class);

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.url.expiry:3600}")
    private int urlExpiry;

    private MinioClient minioClient;

    /**
     * 初始化MinIO客户端
     */
    @PostConstruct
    public void init() {
        log.info("初始化MinIO客户端: endpoint={}, accessKey={}", endpoint, accessKey);
        try {
            minioClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();
            
            log.info("MinIO客户端初始化成功");
        } catch (Exception e) {
            log.error("MinIO客户端初始化失败: {}", e.getMessage(), e);
            throw new RuntimeException("MinIO客户端初始化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 上传文件到MinIO
     * 
     * @param file 上传的文件
     * @param bucketName 存储桶名称
     * @param objectName 对象名称（文件路径）
     * @return 文件访问URL
     * @throws Exception 上传异常
     */
    @Override
    public String uploadFile(MultipartFile file, String bucketName, String objectName) throws Exception {
        log.info("上传文件到MinIO: bucket={}, object={}, filename={}", 
            bucketName, objectName, file.getOriginalFilename());
        
        // 检查存储桶是否存在，不存在则创建
        if (!bucketExists(bucketName)) {
            createBucket(bucketName);
        }
        
        // 上传文件
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build()
        );
        
        // 返回文件访问URL
        return getFileUrl(bucketName, objectName, urlExpiry);
    }

    /**
     * 上传字节数组到MinIO
     * 
     * @param bytes 文件字节数组
     * @param bucketName 存储桶名称
     * @param objectName 对象名称（文件路径）
     * @param contentType 文件类型
     * @return 文件访问URL
     * @throws Exception 上传异常
     */
    @Override
    public String uploadBytes(byte[] bytes, String bucketName, String objectName, String contentType) throws Exception {
        log.info("上传字节数组到MinIO: bucket={}, object={}, size={}, contentType={}", 
            bucketName, objectName, bytes.length, contentType);
        
        // 检查存储桶是否存在，不存在则创建
        if (!bucketExists(bucketName)) {
            createBucket(bucketName);
        }
        
        // 上传字节数组
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .stream(new java.io.ByteArrayInputStream(bytes), bytes.length, -1)
                .contentType(contentType)
                .build()
        );
        
        // 返回文件访问URL
        return getFileUrl(bucketName, objectName, urlExpiry);
    }

    /**
     * 从MinIO下载文件
     * 
     * @param bucketName 存储桶名称
     * @param objectName 对象名称（文件路径）
     * @return 文件输入流
     * @throws Exception 下载异常
     */
    @Override
    public InputStream downloadFile(String bucketName, String objectName) throws Exception {
        log.info("从MinIO下载文件: bucket={}, object={}", bucketName, objectName);
        
        return minioClient.getObject(
            io.minio.GetObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .build()
        );
    }

    /**
     * 从MinIO删除文件
     * 
     * @param bucketName 存储桶名称
     * @param objectName 对象名称（文件路径）
     * @throws Exception 删除异常
     */
    @Override
    public void deleteFile(String bucketName, String objectName) throws Exception {
        log.info("从MinIO删除文件: bucket={}, object={}", bucketName, objectName);
        
        minioClient.removeObject(
            RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .build()
        );
    }

    /**
     * 获取文件访问URL
     * 
     * @param bucketName 存储桶名称
     * @param objectName 对象名称（文件路径）
     * @param expirySeconds 过期时间（秒）
     * @return 预签名URL
     * @throws Exception 获取URL异常
     */
    @Override
    public String getFileUrl(String bucketName, String objectName, int expirySeconds) throws Exception {
        log.info("获取文件访问URL: bucket={}, object={}, expiry={}s", 
            bucketName, objectName, expirySeconds);
        
        return minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucketName)
                .object(objectName)
                .expiry(expirySeconds, TimeUnit.SECONDS)
                .build()
        );
    }

    /**
     * 检查存储桶是否存在
     * 
     * @param bucketName 存储桶名称
     * @return 是否存在
     * @throws Exception 检查异常
     */
    @Override
    public boolean bucketExists(String bucketName) throws Exception {
        log.info("检查存储桶是否存在: bucket={}", bucketName);
        
        return minioClient.bucketExists(
            BucketExistsArgs.builder()
                .bucket(bucketName)
                .build()
        );
    }

    /**
     * 创建存储桶
     * 
     * @param bucketName 存储桶名称
     * @throws Exception 创建异常
     */
    @Override
    public void createBucket(String bucketName) throws Exception {
        log.info("创建存储桶: bucket={}", bucketName);
        
        if (!bucketExists(bucketName)) {
            minioClient.makeBucket(
                MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build()
            );
        }
    }

    /**
     * 获取文件元数据
     * 
     * @param bucketName 存储桶名称
     * @param objectName 对象名称（文件路径）
     * @return 文件元数据，包含contentType等信息
     * @throws Exception 获取元数据异常
     */
    @Override
    public io.minio.StatObjectResponse getFileMetadata(String bucketName, String objectName) throws Exception {
        log.info("获取文件元数据: bucket={}, object={}", bucketName, objectName);
        
        return minioClient.statObject(
            io.minio.StatObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .build()
        );
    }
}
