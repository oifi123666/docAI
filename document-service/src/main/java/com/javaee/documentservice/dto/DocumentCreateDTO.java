package com.javaee.documentservice.dto;

import lombok.Data;

import java.util.List;

/**
 * 文档创建DTO
 * 采用文件ID模式，通过fileId从file-service获取文件内容
 */
@Data
public class DocumentCreateDTO {

    private String title;

    private String fileId;

    private String category;

    private List<String> tags;
}