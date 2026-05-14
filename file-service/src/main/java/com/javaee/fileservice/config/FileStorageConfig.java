package com.javaee.fileservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 文件存储通用配置类
 */
@Configuration
public class FileStorageConfig {

    @Value("${file.storage.type}")
    private String storageType;

    @Value("${file.storage.local-path}")
    private String localPath;

    @Value("${file.storage.max-size}")
    private long maxSize;

    @Value("${minio.bucket-name}")
    private String bucketName;

    public String getStorageType() {
        return storageType;
    }

    public String getLocalPath() {
        return localPath;
    }

    public long getMaxSize() {
        return maxSize;
    }

    public String getBucketName() {
        return bucketName;
    }

}
