package com.javaee.aiservice.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件恢复结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileRestoreVO {
    
    /**
     * 恢复状态
     */
    private String status;
    
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
     * 消息
     */
    private String message;
}
