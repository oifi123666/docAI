package com.javaee.documentservice.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnotationVO {
    private String id;
    private String documentId;
    private Long userId;
    private String userName;
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
