package com.javaee.fileservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件元数据实体类
 */
@Data
@TableName("file_metadata")
public class FileMetadata {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String fileId;

    private String fileName;

    private String originalFileName;

    private String filePath;

    private String fileType;

    private long fileSize;

    private String md5;

    private String storageType;

    private String bucketName;

    private String objectKey;

    private String status;

    private String createBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
