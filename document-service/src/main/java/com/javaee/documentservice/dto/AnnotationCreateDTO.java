package com.javaee.documentservice.dto;

import lombok.Data;

@Data
public class AnnotationCreateDTO {
    private String documentId;
    private Integer lineNumber;
    private Integer startOffset;
    private Integer endOffset;
    private String annotationType;
    private String content;
    private String color;
}
