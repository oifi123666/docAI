package com.javaee.aiservice.controller;

import com.javaee.aiservice.dto.*;
import com.javaee.aiservice.factory.AIServiceFactory;
import com.javaee.aiservice.model.ModelType;
import com.javaee.aiservice.security.RequestUserContext;
import com.javaee.aiservice.service.*;
import com.javaee.aiservice.vo.*;
import com.javaee.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI控制器
 * 提供AI处理和文件管理相关的REST API接口，支持多模型选择
 */
@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI处理", description = "文档摘要、关键词提取、文档分析等AI处理接口")
public class AIController {

    @Autowired
    private AIService aiService;

    @Autowired
    private AIServiceFactory aiServiceFactory;

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private FileDownloadService fileDownloadService;

    @Autowired
    private FileDeleteService fileDeleteService;

    @Autowired
    private FileVersionService fileVersionService;

    @Autowired
    private RecycleBinService recycleBinService;

    @Autowired
    private RequestUserContext requestUserContext;

    /**
     * 获取可用模型列表
     * @return 模型列表
     */
    @GetMapping("/models")
    @Operation(summary = "获取可用模型列表", description = "获取所有可用的AI模型列表及其详细信息")
    public Result<java.util.List<Map<String, Object>>> getModels() {
        java.util.List<Map<String, Object>> models = aiServiceFactory.getAllServices().entrySet().stream()
            .map(entry -> {
                Map<String, Object> model = new HashMap<>();
                model.put("code", entry.getKey().getCode());
                model.put("name", entry.getKey().getName());
                model.put("provider", entry.getKey().getProvider());
                model.put("available", entry.getValue().isAvailable());
                return model;
            })
            .collect(Collectors.toList());
        return Result.success(models);
    }

    /**
     * 文档摘要
     * @param dto 摘要请求参数
     * @param model 模型代码（可选）
     * @return 摘要结果
     */
    @PostMapping("/summarize")
    @Operation(summary = "文档摘要", description = "对文档进行智能摘要，支持选择不同模型：qwen-plus, qwen-max, qwen-turbo, deepseek-v3.2")
    public Result<TextSummarizeVO> summarize(
            @RequestBody TextSummarizeDTO dto,
            @Parameter(description = "选择AI模型（可选，默认qwen-plus）", 
                      in = ParameterIn.QUERY,
                      schema = @Schema(allowableValues = {"qwen-plus", "qwen-max", "qwen-turbo", "deepseek-v3.2"}, 
                                        defaultValue = "qwen-plus"))
            @RequestParam(required = false) String model) {
        TextSummarizeVO vo = aiService.summarize(dto, model);
        return Result.success(vo);
    }

    /**
     * 关键词提取
     * @param dto 关键词提取请求参数
     * @param model 模型代码（可选）
     * @return 关键词结果
     */
    @PostMapping("/keywords")
    @Operation(summary = "关键词提取", description = "从文档中提取关键词，支持选择不同模型：qwen-plus, qwen-max, qwen-turbo, deepseek-v3.2")
    public Result<KeywordExtractVO> extractKeywords(
            @RequestBody KeywordExtractDTO dto,
            @Parameter(description = "选择AI模型（可选，默认qwen-plus）", 
                      in = ParameterIn.QUERY,
                      schema = @Schema(allowableValues = {"qwen-plus", "qwen-max", "qwen-turbo", "deepseek-v3.2"}, 
                                        defaultValue = "qwen-plus"))
            @RequestParam(required = false) String model) {
        KeywordExtractVO vo = aiService.extractKeywords(dto, model);
        return Result.success(vo);
    }

    /**
     * 文档分析
     * @param dto 文档分析请求参数
     * @return 分析结果
     */
    @PostMapping("/analyze")
    @Operation(summary = "文档分析", description = "对文档进行深度分析")
    public Result<TextAnalyzeVO> analyze(@RequestBody TextAnalyzeDTO dto) {
        TextAnalyzeVO vo = aiService.analyze(dto);
        return Result.success(vo);
    }

    /**
     * 文件上传接口
     * @param file 上传的文件
     * @param dto 上传参数
     * @return 文件上传结果
     */
    @PostMapping("/upload")
    @Operation(summary = "文件上传", description = "将文件上传至MinIO服务器")
    public Result<FileUploadVO> uploadFile(
            @Parameter(description = "上传的文件") @RequestParam("file") MultipartFile file,
            FileUploadDTO dto) {
        FileUploadVO vo = fileUploadService.uploadFile(file, dto);
        return Result.success(vo);
    }

    /**
     * 文件下载接口（直接下载）
     * @param dto 下载参数
     * @return 文件流
     */
    @GetMapping("/download")
    @Operation(summary = "文件下载", description = "从MinIO服务器下载文件")
    public ResponseEntity<org.springframework.core.io.Resource> downloadFile(FileDownloadDTO dto) {
        try {
            InputStream inputStream = fileDownloadService.downloadFile(dto);
            
            String filename = dto.getObjectName();
            if (filename.contains("/")) {
                filename = filename.substring(filename.lastIndexOf("/") + 1);
            }
            
            org.springframework.core.io.InputStreamResource resource = 
                new org.springframework.core.io.InputStreamResource(inputStream);
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"" + URLEncoder.encode(filename, StandardCharsets.UTF_8) + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取文件访问URL接口
     * @param dto 下载参数
     * @return 文件访问URL
     */
    @GetMapping("/download/url")
    @Operation(summary = "获取文件URL", description = "获取MinIO中文件的预签名访问URL")
    public Result<FileDownloadVO> getFileUrl(FileDownloadDTO dto) {
        FileDownloadVO vo = fileDownloadService.getFileUrl(dto);
        return Result.success(vo);
    }

    /**
     * 删除文件接口
     * @param dto 删除参数
     * @return 删除结果
     */
    @DeleteMapping("/files")
    @Operation(summary = "删除文件", description = "删除文件，支持确认删除和回收站")
    public Result<FileDeleteVO> deleteFile(FileDeleteDTO dto) {
        if (dto.getConfirmationToken() == null || dto.getConfirmationToken().isBlank()) {
            dto.setRequireConfirmation(true);
        }
        FileDeleteVO vo = fileDeleteService.deleteFile(dto, requestUserContext.getRequiredUserId());
        return Result.success(vo);
    }

    /**
     * 恢复文件接口
     * @param dto 恢复参数
     * @return 恢复结果
     */
    @PostMapping("/files/restore")
    @Operation(summary = "恢复文件", description = "从回收站恢复文件")
    public Result<FileRestoreVO> restoreFile(@RequestBody FileRestoreDTO dto) {
        FileRestoreVO vo = fileDeleteService.restoreFile(dto);
        return Result.success(vo);
    }

    /**
     * 获取回收站文件列表接口
     * @param bucketName 存储桶名称
     * @return 回收站文件列表
     */
    @GetMapping("/recycle-bin")
    @Operation(summary = "回收站列表", description = "获取回收站中的文件列表")
    public Result<RecycleBinVO> listRecycleBin(
            @Parameter(description = "存储桶名称（可选）") @RequestParam(required = false) String bucketName) {
        RecycleBinVO vo = recycleBinService.listRecycleBin(bucketName, requestUserContext.getRequiredUserId());
        return Result.success(vo);
    }

    /**
     * 获取文件版本列表接口
     * @param dto 查询参数
     * @return 文件版本信息
     */
    @GetMapping("/files/versions")
    @Operation(summary = "文件版本列表", description = "获取文件的所有版本")
    public Result<FileVersionVO> getVersions(FileVersionDTO dto) {
        FileVersionVO vo = fileVersionService.getVersions(dto);
        return Result.success(vo);
    }

    /**
     * 切换文件版本接口
     * @param dto 切换参数
     * @return 切换后的版本信息
     */
    @PostMapping("/files/versions/switch")
    @Operation(summary = "切换文件版本", description = "切换到指定版本")
    public Result<FileVersionVO.VersionInfo> switchVersion(@RequestBody FileVersionSwitchDTO dto) {
        if (!requestUserContext.isAdmin()) {
            throw new SecurityException("切换文件版本属于高风险操作，请通过Agent二次确认或管理员接口执行");
        }
        FileVersionVO.VersionInfo vo = fileVersionService.switchVersion(dto);
        return Result.success(vo);
    }
}
