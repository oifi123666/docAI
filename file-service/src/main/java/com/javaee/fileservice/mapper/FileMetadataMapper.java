package com.javaee.fileservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.javaee.fileservice.entity.FileMetadata;

/**
 * 文件元数据数据访问接口
 */
public interface FileMetadataMapper extends BaseMapper<FileMetadata> {

    /**
     * 根据文件ID获取文件元数据
     */
    FileMetadata selectByFileId(String fileId);

    /**
     * 根据文件名搜索文件
     */
    java.util.List<FileMetadata> searchByFileName(String keyword);

}
