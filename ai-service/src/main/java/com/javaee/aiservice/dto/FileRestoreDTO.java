package com.javaee.aiservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件恢复请求参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileRestoreDTO {
    
    /**
     * 回收站记录ID
     */
    private String recycleId;
    
    /**
     * 存储桶名称
     */
    private String bucketName;
    
    /**
     * 对象名称（文件路径）
     */
    private String objectName;
    
    /**
     * 恢复到的新对象名称（可选，不填则恢复到原位置）
     */
    private String newObjectName;
}
