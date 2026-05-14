package com.javaee.fileservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.javaee.fileservice.entity.FileMetadata;
import com.javaee.fileservice.mapper.FileMetadataMapper;
import com.javaee.fileservice.service.FileMetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件元数据服务实现类
 */
@Service
public class FileMetadataServiceImpl implements FileMetadataService {

    @Autowired
    private FileMetadataMapper fileMetadataMapper;

    @Override
    public FileMetadata getMetadata(String fileId) {
        try {
            return fileMetadataMapper.selectByFileId(fileId);
        } catch (Exception e) {
            System.out.println("获取元数据失败: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void saveMetadata(FileMetadata fileMetadata) {
        try {
            fileMetadata.setCreateTime(LocalDateTime.now());
            fileMetadata.setUpdateTime(LocalDateTime.now());
            fileMetadata.setStatus("ACTIVE");
            fileMetadataMapper.insert(fileMetadata);
        } catch (Exception e) {
            System.out.println("保存元数据失败: " + e.getMessage());
        }
    }

    @Override
    public void updateMetadata(FileMetadata fileMetadata) {
        try {
            fileMetadata.setUpdateTime(LocalDateTime.now());
            QueryWrapper<FileMetadata> wrapper = new QueryWrapper<>();
            wrapper.eq("file_id", fileMetadata.getFileId());
            fileMetadataMapper.update(fileMetadata, wrapper);
        } catch (Exception e) {
            System.out.println("更新元数据失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteMetadata(String fileId) {
        try {
            QueryWrapper<FileMetadata> wrapper = new QueryWrapper<>();
            wrapper.eq("file_id", fileId);
            fileMetadataMapper.delete(wrapper);
        } catch (Exception e) {
            System.out.println("删除元数据失败: " + e.getMessage());
        }
    }

    @Override
    public List<FileMetadata> getFileList(int page, int size, String sortBy, String direction) {
        try {
            Page<FileMetadata> pageObj = new Page<>(page, size);
            QueryWrapper<FileMetadata> wrapper = new QueryWrapper<>();
            
            if (sortBy != null && !sortBy.isEmpty()) {
                String dbColumn = camelToSnake(sortBy);
                if ("asc".equals(direction)) {
                    wrapper.orderByAsc(dbColumn);
                } else {
                    wrapper.orderByDesc(dbColumn);
                }
            } else {
                wrapper.orderByDesc("create_time");
            }
            
            Page<FileMetadata> result = fileMetadataMapper.selectPage(pageObj, wrapper);
            return result.getRecords();
        } catch (Exception e) {
            System.out.println("获取文件列表失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    private String camelToSnake(String str) {
        return str.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    @Override
    public List<FileMetadata> searchFiles(String keyword, int page, int size) {
        try {
            Page<FileMetadata> pageObj = new Page<>(page, size);
            QueryWrapper<FileMetadata> wrapper = new QueryWrapper<>();
            wrapper.like("file_name", keyword).or().like("original_file_name", keyword);
            wrapper.orderByDesc("create_time");
            
            Page<FileMetadata> result = fileMetadataMapper.selectPage(pageObj, wrapper);
            return result.getRecords();
        } catch (Exception e) {
            System.out.println("搜索文件失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public Object getDirectoryStructure(String path) {
        try {
            // 简单实现目录结构
            Map<String, Object> structure = new HashMap<>();
            List<Map<String, Object>> files = new ArrayList<>();
            List<Map<String, Object>> directories = new ArrayList<>();
            
            // 这里可以根据实际存储情况实现目录结构的获取
            // 目前返回一个示例结构
            Map<String, Object> dir1 = new HashMap<>();
            dir1.put("name", "documents");
            dir1.put("type", "directory");
            dir1.put("path", path + (path.endsWith("/") ? "" : "/") + "documents");
            directories.add(dir1);
            
            Map<String, Object> dir2 = new HashMap<>();
            dir2.put("name", "images");
            dir2.put("type", "directory");
            dir2.put("path", path + (path.endsWith("/") ? "" : "/") + "images");
            directories.add(dir2);
            
            structure.put("directories", directories);
            structure.put("files", files);
            structure.put("currentPath", path);
            
            return structure;
        } catch (Exception e) {
            System.out.println("获取目录结构失败: " + e.getMessage());
            // 返回空的目录结构
            Map<String, Object> structure = new HashMap<>();
            structure.put("directories", new ArrayList<>());
            structure.put("files", new ArrayList<>());
            structure.put("currentPath", path);
            return structure;
        }
    }

}
