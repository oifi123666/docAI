package com.javaee.aiservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件版本切换请求参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileVersionSwitchDTO {
    
    /**
     * 存储桶名称
     */
    private String bucketName;
    
    /**
     * 对象名称（文件路径）
     */
    private String objectName;
    
    /**
     * 目标版本号
     */
    private String targetVersionId;
}
