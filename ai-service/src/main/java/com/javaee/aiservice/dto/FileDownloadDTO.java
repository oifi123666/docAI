package com.javaee.aiservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件下载请求参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileDownloadDTO {
    
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
     * 是否直接下载（true返回文件流，false返回URL）
     */
    private Boolean directDownload = true;
}
