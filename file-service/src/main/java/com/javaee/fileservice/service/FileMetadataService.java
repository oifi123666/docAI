package com.javaee.fileservice.service;

import com.javaee.fileservice.entity.FileMetadata;
import java.util.List;

/**
 * 文件元数据服务接口
 */
public interface FileMetadataService {

    /**
     * 根据文件ID获取元数据
     */
    FileMetadata getMetadata(String fileId);

    /**
     * 保存文件元数据
     */
    void saveMetadata(FileMetadata fileMetadata);

    /**
     * 更新文件元数据
     */
    void updateMetadata(FileMetadata fileMetadata);

    /**
     * 删除文件元数据
     */
    void deleteMetadata(String fileId);

    /**
     * 获取文件列表
     */
    List<FileMetadata> getFileList(int page, int size, String sortBy, String direction);

    /**
     * 搜索文件
     */
    List<FileMetadata> searchFiles(String keyword, int page, int size);

    /**
     * 获取目录结构
     */
    Object getDirectoryStructure(String path);

} 
