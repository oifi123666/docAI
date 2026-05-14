package com.javaee.aiservice.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文件版本信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileVersionVO {
    
    /**
     * 存储桶名称
     */
    private String bucketName;
    
    /**
     * 对象名称（文件路径）
     */
    private String objectName;
    
    /**
     * 当前版本号
     */
    private String currentVersionId;
    
    /**
     * 版本列表
     */
    private List<VersionInfo> versions;
    
    /**
     * 版本信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VersionInfo {
        
        /**
         * 版本号
         */
        private String versionId;
        
        /**
         * 是否为当前版本
         */
        private Boolean isCurrent;
        
        /**
         * 创建时间
         */
        private Long createTime;
        
        /**
         * 文件大小
         */
        private Long fileSize;
        
        /**
         * 上传者
         */
        private String uploader;
        
        /**
         * 版本备注
         */
        private String remark;
    }
}
