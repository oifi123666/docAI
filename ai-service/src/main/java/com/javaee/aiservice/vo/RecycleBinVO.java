package com.javaee.aiservice.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 回收站文件列表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecycleBinVO {
    
    /**
     * 回收站文件总数
     */
    private Integer totalCount;
    
    /**
     * 回收站文件列表
     */
    private List<RecycleFile> files;
    
    /**
     * 回收站文件信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecycleFile {
        
        /**
         * 回收站记录ID
         */
        private String recycleId;
        
        /**
         * 存储桶名称
         */
        private String bucketName;
        
        /**
         * 原始对象名称
         */
        private String originalObjectName;
        
        /**
         * 删除时间
         */
        private Long deleteTime;
        
        /**
         * 过期时间（超过后自动永久删除）
         */
        private Long expiryTime;
        
        /**
         * 文件大小
         */
        private Long fileSize;
        
        /**
         * 删除者
         */
        private String deleter;
    }
}
