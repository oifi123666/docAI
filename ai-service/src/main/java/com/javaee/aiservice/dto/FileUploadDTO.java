package com.javaee.aiservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件上传请求参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadDTO {
    
    /**
     * 存储桶名称
     * 如果为空，使用默认存储桶
     */
    private String bucketName;
    
    /**
     * 对象名称（文件路径）
     * 如果为空，自动生成文件名
     */
    private String objectName;
}
