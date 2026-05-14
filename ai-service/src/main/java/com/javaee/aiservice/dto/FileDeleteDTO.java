package com.javaee.aiservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件删除请求参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileDeleteDTO {
    
    /**
     * 存储桶名称
     * 如果为空，使用默认存储桶
     */
    private String bucketName;
    
    /**
     * 对象名称（文件路径）
     * 必填
     */
    private String objectName;
    
    /**
     * 是否需要确认（true需要确认，false直接删除）
     */
    private Boolean requireConfirmation = true;
    
    /**
     * 确认token（需要确认时使用）
     */
    private String confirmationToken;
}
