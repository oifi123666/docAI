package com.javaee.documentservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档版本实体类
 */
@Data
@TableName("document_version")
public class DocumentVersion {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String documentId;

    private Integer versionNumber;

    private String title;

    private String content;

    private String summary;

    private String keywords;

    private String changeLog;

    private String createdBy;

    private LocalDateTime createTime;
}
