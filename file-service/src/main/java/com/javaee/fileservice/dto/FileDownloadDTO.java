package com.javaee.fileservice.dto;

import lombok.Data;

/**
 * 文件下载请求参数
 */
@Data
public class FileDownloadDTO {

    private String fileId;

    private boolean preview;

    private String range;

}
