package com.javaee.aiservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件版本查询参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileVersionDTO {
    
    /**
     * 存储桶名称
     */
    private String bucketName;
    
    /**
     * 对象名称（文件路径）
     */
    private String objectName;
    
    /**
     * 版本号（可选，用于指定版本）
     */
    private String versionId;
}
