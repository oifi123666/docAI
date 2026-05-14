package com.javaee.aiservice.dto;

import lombok.Data;

@Data
public class TextSummarizeDTO {
    private String content;
    private Integer maxLength;
}
