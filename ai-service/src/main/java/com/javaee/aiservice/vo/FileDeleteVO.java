package com.javaee.aiservice.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件删除结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileDeleteVO {
    
    /**
     * 删除状态（pending等待确认，deleted已删除，recycle已移至回收站）
     */
    private String status;
    
    /**
     * 确认token（需要确认时返回）
     */
    private String confirmationToken;
    
    /**
     * 确认过期时间（时间戳）
     */
    private Long confirmationExpiry;
    
    /**
     * 回收站记录ID（移至回收站时返回）
     */
    private String recycleId;
    
    /**
     * 消息
     */
    private String message;
}
