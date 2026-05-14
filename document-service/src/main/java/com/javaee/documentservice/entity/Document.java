package com.javaee.documentservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档实体类
 */
@Data
@TableName("document")
public class Document {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String title;

    private String content;

    private String summary;

    private String keywords;

    private String fileId;

    private Long userId;

    private String status;

    private Integer version;

    private String category;

    private String tags;

    private String createdBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
