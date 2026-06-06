package com.javaee.fileservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.javaee.fileservice.service.FileMetadataService;
import com.javaee.fileservice.entity.FileMetadata;
import java.util.List;
import java.util.Map;

/**
 * 文件元数据查询接口控制器
 */
@RestController
@RequestMapping("/api/files")
@Tag(name = "文件元数据", description = "文件元数据查询接口")
public class FileMetadataController {

    @Autowired
    private FileMetadataService fileMetadataService;

    /**
     * 获取文件元数据
     */
    @GetMapping("/metadata/{fileId}")
    @Operation(summary = "获取文件元数据", description = "根据文件ID获取文件详细信息")
    public ResponseEntity<?> getMetadata(@Parameter(description = "文件ID") @PathVariable String fileId) {
        try {
            FileMetadata metadata = fileMetadataService.getMetadata(fileId);
            if (metadata == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(metadata);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("获取元数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取文件列表", description = "获取文件列表，支持分页和排序")
    public ResponseEntity<?> getFileList(@Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
                           @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
                           @Parameter(description = "排序字段") @RequestParam(defaultValue = "createTime") String sortBy,
                           @Parameter(description = "排序方向") @RequestParam(defaultValue = "desc") String direction) {
        try {
            List<FileMetadata> fileList = fileMetadataService.getFileList(page, size, sortBy, direction);
            return ResponseEntity.ok(fileList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("获取文件列表失败: " + e.getMessage());
        }
    }

    /**
     * 搜索文件
     */
    @GetMapping("/search")
    @Operation(summary = "搜索文件", description = "根据关键词搜索文件")
    public ResponseEntity<?> searchFiles(@Parameter(description = "搜索关键词") @RequestParam("keyword") String keyword,
                          @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
                          @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {
        try {
            List<FileMetadata> searchResults = fileMetadataService.searchFiles(keyword, page, size);
            return ResponseEntity.ok(searchResults);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("搜索文件失败: " + e.getMessage());
        }
    }

    /**
     * 获取目录结构
     */
    @GetMapping("/directory")
    @Operation(summary = "获取目录结构", description = "获取文件系统目录结构")
    public ResponseEntity<?> getDirectoryStructure(@Parameter(description = "目录路径") @RequestParam(defaultValue = "/") String path) {
        try {
            Object directoryStructure = fileMetadataService.getDirectoryStructure(path);
            return ResponseEntity.ok(directoryStructure);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("获取目录结构失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件名
     */
    @GetMapping("/metadata/{fileId}/name")
    @Operation(summary = "获取文件名", description = "根据文件ID获取文件名")
    public ResponseEntity<?> getFileName(@Parameter(description = "文件ID") @PathVariable String fileId) {
        try {
            FileMetadata metadata = fileMetadataService.getMetadata(fileId);
            if (metadata == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(metadata.getFileName());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("获取文件名失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件类型
     */
    @GetMapping("/metadata/{fileId}/type")
    @Operation(summary = "获取文件类型", description = "根据文件ID获取文件类型")
    public ResponseEntity<?> getFileType(@Parameter(description = "文件ID") @PathVariable String fileId) {
        try {
            FileMetadata metadata = fileMetadataService.getMetadata(fileId);
            if (metadata == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(metadata.getFileType());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("获取文件类型失败: " + e.getMessage());
        }
    }

}
