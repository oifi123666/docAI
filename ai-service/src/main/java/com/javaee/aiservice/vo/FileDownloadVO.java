package com.javaee.aiservice.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件下载结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileDownloadVO {
    
    /**
     * 文件访问URL（当directDownload=false时返回）
     */
    private String fileUrl;
    
    /**
     * 存储桶名称
     */
    private String bucketName;
    
    /**
     * 对象名称（文件路径）
     */
    private String objectName;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 过期时间（秒）
     */
    private Integer expirySeconds;
}
