package com.javaee.documentservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("document_comment")
public class DocumentComment {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String documentId;

    private Long userId;

    private String content;

    private String parentId;

    private String createdBy;

    private LocalDateTime createTime;

    private String status;
}
