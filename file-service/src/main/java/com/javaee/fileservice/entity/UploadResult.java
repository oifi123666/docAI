package com.javaee.fileservice.entity;

import lombok.Data;

/**
 * 上传结果封装类
 */
@Data
public class UploadResult {

    private String fileId;

    private String fileName;

    private String filePath;

    private long fileSize;

    private String fileType;

    private String md5;

    private String storageType;

    private boolean success;

    private String message;

}
