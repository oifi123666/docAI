package com.javaee.aiservice.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * MinIO服务接口
 * 功能说明：提供文件上传、下载、删除等操作
 * 待实现：后续需要实现minIO客户端的具体功能
 */
public interface MinIOService {

    /**
     * 上传文件到MinIO
     * 
     * @param file 上传的文件
     * @param bucketName 存储桶名称
     * @param objectName 对象名称（文件路径）
     * @return 文件访问URL
     * @throws Exception 上传异常
     */
    String uploadFile(MultipartFile file, String bucketName, String objectName) throws Exception;

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
    String uploadBytes(byte[] bytes, String bucketName, String objectName, String contentType) throws Exception;

    /**
     * 从MinIO下载文件
     * 
     * @param bucketName 存储桶名称
     * @param objectName 对象名称（文件路径）
     * @return 文件输入流
     * @throws Exception 下载异常
     */
    InputStream downloadFile(String bucketName, String objectName) throws Exception;

    /**
     * 从MinIO删除文件
     * 
     * @param bucketName 存储桶名称
     * @param objectName 对象名称（文件路径）
     * @throws Exception 删除异常
     */
    void deleteFile(String bucketName, String objectName) throws Exception;

    /**
     * 获取文件访问URL
     * 
     * @param bucketName 存储桶名称
     * @param objectName 对象名称（文件路径）
     * @param expirySeconds 过期时间（秒）
     * @return 预签名URL
     * @throws Exception 获取URL异常
     */
    String getFileUrl(String bucketName, String objectName, int expirySeconds) throws Exception;

    /**
     * 检查存储桶是否存在
     * 
     * @param bucketName 存储桶名称
     * @return 是否存在
     * @throws Exception 检查异常
     */
    boolean bucketExists(String bucketName) throws Exception;

    /**
     * 创建存储桶
     * 
     * @param bucketName 存储桶名称
     * @throws Exception 创建异常
     */
    void createBucket(String bucketName) throws Exception;

    /**
     * 获取文件元数据
     * 
     * @param bucketName 存储桶名称
     * @param objectName 对象名称（文件路径）
     * @return 文件元数据，包含contentType等信息
     * @throws Exception 获取元数据异常
     */
    io.minio.StatObjectResponse getFileMetadata(String bucketName, String objectName) throws Exception;
}
