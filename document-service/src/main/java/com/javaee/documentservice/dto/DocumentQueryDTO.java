package com.javaee.documentservice.dto;

import lombok.Data;

/**
 * 文档查询DTO
 */
@Data
public class DocumentQueryDTO {

    private String title;

    private String category;

    private String keyword;

    private Integer pageNum;

    private Integer pageSize;
}
