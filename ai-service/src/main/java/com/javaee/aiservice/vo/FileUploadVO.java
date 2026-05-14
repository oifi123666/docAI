package com.javaee.aiservice.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件上传结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadVO {
    
    /**
     * 文件访问URL
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
     * 原始文件名
     */
    private String originalFilename;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 文件类型
     */
    private String contentType;
    
    /**
     * 上传时间戳
     */
    private Long uploadTime;
}
