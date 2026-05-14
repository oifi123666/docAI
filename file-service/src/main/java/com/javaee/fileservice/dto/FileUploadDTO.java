package com.javaee.fileservice.dto;

import lombok.Data;

/**
 * 文件上传请求参数
 */
@Data
public class FileUploadDTO {

    private String fileName;

    private String fileType;

    private String directory;

    private boolean override;

}
