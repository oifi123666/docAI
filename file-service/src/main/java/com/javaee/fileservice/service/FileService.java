package com.javaee.fileservice.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务接口
 */
public interface FileService {

    /**
     * 单文件上传
     */
    String upload(MultipartFile file);

    /**
     * 多文件上传
     */
    String[] uploadMultiple(MultipartFile[] files);

    /**
     * 分片上传
     */
    void uploadChunk(MultipartFile chunk, String fileId, int chunkIndex, int totalChunks);

    /**
     * 分片合并
     */
    String mergeChunk(String fileId, String fileName);

    /**
     * 文件下载
     */
    byte[] download(String fileId);

    /**
     * 文件删除
     */
    void delete(String fileId);

    /**
     * 文件重命名
     */
    void rename(String fileId, String newName);

    /**
     * 文件移动
     */
    void move(String fileId, String targetPath);

    /**
     * 文件复制
     */
    String copy(String fileId, String targetPath);

}
