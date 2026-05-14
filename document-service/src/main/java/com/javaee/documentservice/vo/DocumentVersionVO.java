package com.javaee.documentservice.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档版本VO
 */
@Data
public class DocumentVersionVO {

    private String id;

    private String documentId;

    private Integer versionNumber;

    private String title;

    private String content;

    private String summary;

    private List<String> keywords;

    private String changeLog;

    private String createdBy;

    private LocalDateTime createTime;
}
