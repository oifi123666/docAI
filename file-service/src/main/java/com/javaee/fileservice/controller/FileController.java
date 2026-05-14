package com.javaee.fileservice.controller;

import com.javaee.common.model.Result;
import com.javaee.fileservice.config.RabbitMQConfig;
import com.javaee.fileservice.service.FileService;
import com.javaee.fileservice.util.RabbitMQUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件核心接口控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
@Tag(name = "文件管理", description = "文件上传、下载、删除、分片等核心接口")
public class FileController {

    @Autowired
    private FileService fileService;

    @Autowired
    private RabbitMQUtil rabbitMQUtil;

    /**
     * 单文件上传
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "单文件上传", description = "上传单个文件到服务器")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file,
                                              @RequestHeader(value = "X-User-Id", required = false) String userId) {
        log.info("=== 收到文件上传请求 ===");
        log.info("文件名: {}, 文件大小: {}, 文件类型: {}", 
            file.getOriginalFilename(), file.getSize(), file.getContentType());
        try {
            String fileId = fileService.upload(file);
            log.info("文件上传成功，文件ID: {}", fileId);
            
            Map<String, Object> message = new HashMap<>();
            message.put("fileId", fileId);
            message.put("fileName", file.getOriginalFilename());
            message.put("fileSize", file.getSize());
            message.put("userId", userId);
            message.put("timestamp", LocalDateTime.now().toString());
            
            log.info("发送文件上传消息到 RabbitMQ");
            rabbitMQUtil.send(RabbitMQConfig.FILE_EXCHANGE, RabbitMQConfig.FILE_UPLOAD_ROUTING_KEY, message);
            
            return Result.success(Map.of("fileId", fileId, "message", "文件上传成功"));
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            return Result.fail("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 多文件上传
     */
    @PostMapping(value = "/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "多文件上传", description = "上传多个文件到服务器")
    public Result<Map<String, Object>> uploadMultiple(@RequestParam("files") MultipartFile[] files) {
        System.out.println("=== 收到多文件上传请求 ===");
        System.out.println("文件数量: " + files.length);
        try {
            System.out.println("调用fileService.uploadMultiple方法");
            String[] fileIds = fileService.uploadMultiple(files);
            System.out.println("多文件上传成功，文件ID数量: " + fileIds.length);
            return Result.success(Map.of("fileIds", fileIds, "message", "文件上传成功", "count", fileIds.length));
        } catch (Exception e) {
            System.out.println("多文件上传失败: " + e.getMessage());
            e.printStackTrace();
            return Result.fail("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 分片上传
     */
    @PostMapping(value = "/upload-chunk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "分片上传", description = "大文件分片上传")
    public ResponseEntity<Map<String, String>> uploadChunk(@RequestParam("chunk") MultipartFile chunk,
                           @RequestParam("fileId") String fileId,
                           @RequestParam("chunkIndex") int chunkIndex,
                           @RequestParam("totalChunks") int totalChunks) {
        try {
            fileService.uploadChunk(chunk, fileId, chunkIndex, totalChunks);
            return ResponseEntity.ok(Map.of("message", "分片上传成功", "chunkIndex", String.valueOf(chunkIndex)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "分片上传失败: " + e.getMessage()));
        }
    }

    /**
     * 分片合并
     */
    @PostMapping("/merge-chunk")
    @Operation(summary = "分片合并", description = "合并文件分片")
    public ResponseEntity<Map<String, String>> mergeChunk(@Parameter(description = "文件唯一标识") @RequestParam("fileId") String fileId,
                          @Parameter(description = "文件名") @RequestParam("fileName") String fileName) {
        try {
            String mergedFileId = fileService.mergeChunk(fileId, fileName);
            return ResponseEntity.ok(Map.of("fileId", mergedFileId, "message", "文件合并成功"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "文件合并失败: " + e.getMessage()));
        }
    }

    /**
     * 文件下载
     */
    @GetMapping("/download/{fileId}")
    @Operation(summary = "文件下载", description = "下载指定文件")
    public ResponseEntity<byte[]> download(@Parameter(description = "文件ID") @PathVariable String fileId,
                                           @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            byte[] fileBytes = fileService.download(fileId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "file_" + fileId);
            
            Map<String, Object> message = new HashMap<>();
            message.put("fileId", fileId);
            message.put("userId", userId);
            message.put("timestamp", LocalDateTime.now().toString());
            
            log.info("发送文件下载消息到 RabbitMQ");
            rabbitMQUtil.send(RabbitMQConfig.FILE_EXCHANGE, RabbitMQConfig.FILE_DOWNLOAD_ROUTING_KEY, message);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileBytes);
        } catch (Exception e) {
            log.error("文件下载失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("文件下载失败: " + e.getMessage()).getBytes());
        }
    }

    /**
     * 文件预览
     */
    @GetMapping("/preview/{fileId}")
    @Operation(summary = "文件预览", description = "预览指定文件")
    public ResponseEntity<byte[]> preview(@Parameter(description = "文件ID") @PathVariable String fileId) {
        try {
            byte[] fileBytes = fileService.download(fileId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileBytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("文件预览失败: " + e.getMessage()).getBytes());
        }
    }

    /**
     * 文件删除
     */
    @DeleteMapping("/{fileId}")
    @Operation(summary = "文件删除", description = "删除指定文件")
    public ResponseEntity<Map<String, String>> delete(@Parameter(description = "文件ID") @PathVariable String fileId,
                                                      @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            fileService.delete(fileId);
            
            Map<String, Object> message = new HashMap<>();
            message.put("fileId", fileId);
            message.put("userId", userId);
            message.put("timestamp", LocalDateTime.now().toString());
            
            log.info("发送文件删除消息到 RabbitMQ");
            rabbitMQUtil.send(RabbitMQConfig.FILE_EXCHANGE, RabbitMQConfig.FILE_DELETE_ROUTING_KEY, message);
            
            return ResponseEntity.ok(Map.of("message", "文件删除成功"));
        } catch (Exception e) {
            log.error("文件删除失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "文件删除失败: " + e.getMessage()));
        }
    }

    /**
     * 文件重命名
     */
    @PutMapping("/{fileId}/rename")
    @Operation(summary = "文件重命名", description = "重命名指定文件")
    public ResponseEntity<Map<String, String>> rename(@Parameter(description = "文件ID") @PathVariable String fileId,
                      @Parameter(description = "新文件名") @RequestParam("newName") String newName) {
        try {
            fileService.rename(fileId, newName);
            return ResponseEntity.ok(Map.of("message", "文件重命名成功"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "文件重命名失败: " + e.getMessage()));
        }
    }

    /**
     * 文件移动
     */
    @PutMapping("/{fileId}/move")
    @Operation(summary = "文件移动", description = "移动文件到指定目录")
    public ResponseEntity<Map<String, String>> move(@Parameter(description = "文件ID") @PathVariable String fileId,
                    @Parameter(description = "目标目录") @RequestParam("targetPath") String targetPath) {
        try {
            fileService.move(fileId, targetPath);
            return ResponseEntity.ok(Map.of("message", "文件移动成功"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "文件移动失败: " + e.getMessage()));
        }
    }

    /**
     * 文件复制
     */
    @PutMapping("/{fileId}/copy")
    @Operation(summary = "文件复制", description = "复制文件到指定目录")
    public ResponseEntity<Map<String, String>> copy(@Parameter(description = "文件ID") @PathVariable String fileId,
                    @Parameter(description = "目标目录") @RequestParam("targetPath") String targetPath) {
        try {
            String newFileId = fileService.copy(fileId, targetPath);
            return ResponseEntity.ok(Map.of("fileId", newFileId, "message", "文件复制成功"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "文件复制失败: " + e.getMessage()));
        }
    }

}
