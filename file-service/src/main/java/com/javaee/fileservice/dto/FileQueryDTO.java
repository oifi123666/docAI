package com.javaee.fileservice.dto;

import lombok.Data;

/**
 * 文件查询请求参数
 */
@Data
public class FileQueryDTO {

    private String keyword;

    private String directory;

    private String fileType;

    private String storageType;

    private Integer page;

    private Integer size;

    private String sortBy;

    private String direction;

}
