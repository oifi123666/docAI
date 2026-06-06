package com.javaee.documentservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档协作访问关系。
 */
@Data
@TableName("document_access")
public class DocumentAccess {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String documentId;

    private String bucketName;

    private Long userId;

    private String role;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
