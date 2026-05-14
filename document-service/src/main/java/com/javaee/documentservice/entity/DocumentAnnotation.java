package com.javaee.documentservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("document_annotation")
public class DocumentAnnotation {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String documentId;

    private Long userId;

    private Integer lineNumber;

    private Integer startOffset;

    private Integer endOffset;

    private String annotationType;

    private String content;

    private String color;

    private String status;

    private String createdBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
