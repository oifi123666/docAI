package com.javaee.documentservice.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档VO
 */
@Data
public class DocumentVO {

    private String id;

    private String title;

    private String content;

    private String summary;

    private List<String> keywords;

    private String fileId;

    private Long userId;

    private String bucketName;

    private String objectName;

    private String category;

    private List<String> tags;

    private Integer version;

    private String status;

    private String createdBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
