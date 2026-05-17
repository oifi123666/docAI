package com.javaee.aiservice.controller;

import com.javaee.aiservice.dto.HtmlPptRequest;
import com.javaee.aiservice.skills.SkillExecutorService;
import com.javaee.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import com.javaee.aiservice.service.MinIOService;

@RestController
@RequestMapping("/api/skills")
@Tag(name = "技能管理", description = "AI技能执行接口")
public class SkillController {

    private final SkillExecutorService skillExecutorService;
    private final MinIOService minIOService;

    @Autowired
    public SkillController(SkillExecutorService skillExecutorService, MinIOService minIOService) {
        this.skillExecutorService = skillExecutorService;
        this.minIOService = minIOService;
    }

    /**
     * 执行文件上传技能（直接文件上传模式）
     * @param file 上传的文件
     * @param bucketName 存储桶名称
     * @param objectName 文件名称
     * @return 上传结果
     */
    @PostMapping(value = "/file/upload", consumes = "multipart/form-data")
    @Operation(summary = "执行文件上传技能", description = "使用技能上传文件到MinIO服务器（直接文件上传模式）")
    public Result<?> executeFileUploadSkill(
            @Parameter(description = "上传的文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "存储桶名称（可选）") @RequestParam(required = false) String bucketName,
            @Parameter(description = "文件名称（可选）") @RequestParam(required = false) String objectName) {
        // 直接使用文件上传，不使用JSON格式
        Object result = skillExecutorService.executeSkill("File Upload Skill", file, bucketName, objectName);
        return Result.success(result);
    }

    /**
     * 执行文件下载技能（直接返回文件流）
     * @param objectName 对象名称
     * @param bucketName 存储桶名称
     * @return 文件流
     */
    @GetMapping("/file/download")
    @Operation(
        summary = "执行文件下载技能", 
        description = "使用技能从MinIO服务器下载文件到本地电脑"
    )
    public ResponseEntity<org.springframework.core.io.Resource> executeFileDownloadSkill(
            @Parameter(description = "对象名称") @RequestParam("objectName") String objectName,
            @Parameter(description = "存储桶名称（可选）") @RequestParam(required = false) String bucketName) {
        System.out.println("接收到文件下载请求: objectName=" + objectName + ", bucketName=" + bucketName);
        try {
            System.out.println("开始执行文件下载技能...");
            // 执行技能获取文件流和元数据
            Object result = skillExecutorService.executeSkill("File Download Skill", objectName, bucketName);
            System.out.println("执行技能成功，获取到结果");
            
            // 检查结果类型
            if (!(result instanceof Object[])) {
                throw new RuntimeException("技能返回类型错误，期望Object[]，实际得到: " + result.getClass().getName());
            }
            
            Object[] resultArray = (Object[]) result;
            if (resultArray.length < 3) {
                throw new RuntimeException("技能返回数组长度错误，期望至少3，实际得到: " + resultArray.length);
            }
            
            // 提取文件流、内容类型和对象名称
            InputStream inputStream = (InputStream) resultArray[0];
            String contentType = (String) resultArray[1];
            String originalObjectName = (String) resultArray[2];
            String bucketMessage = resultArray.length > 3 ? (String) resultArray[3] : null;
            String actualBucketName = resultArray.length > 4 ? (String) resultArray[4] : bucketName;
            
            // 打印桶消息
            if (bucketMessage != null) {
                System.out.println("桶信息: " + bucketMessage);
            }
            System.out.println("实际使用的桶: " + actualBucketName);
            
            // 提取文件名
            String filename = originalObjectName;
            if (filename.contains("/")) {
                filename = filename.substring(filename.lastIndexOf("/") + 1);
            }
            
            // 根据内容类型确定文件扩展名
            String fileExtension = getFileExtensionFromContentType(contentType);
            if (!filename.contains(".") && fileExtension != null) {
                filename = filename + "." + fileExtension;
            }
            
            // 包装为资源
            org.springframework.core.io.InputStreamResource resource = 
                new org.springframework.core.io.InputStreamResource(inputStream);
            
            // 确定媒体类型
            MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
            try {
                mediaType = MediaType.parseMediaType(contentType);
            } catch (Exception e) {
                // 如果无法解析内容类型，使用默认值
            }
            
            // 构建响应头
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=\"" + URLEncoder.encode(filename, StandardCharsets.UTF_8) + "\"");
            
            // 添加桶信息到响应头
            if (bucketMessage != null) {
                headers.add("X-Bucket-Message", URLEncoder.encode(bucketMessage, StandardCharsets.UTF_8));
            }
            headers.add("X-Actual-Bucket", actualBucketName);
            
            // 返回文件流，浏览器会自动下载到本地
            return ResponseEntity.ok()
                .headers(headers)
                .contentType(mediaType)
                .body(resource);
        } catch (Exception e) {
            // 记录异常信息
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 根据内容类型获取文件扩展名
     * @param contentType 内容类型
     * @return 文件扩展名
     */
    private String getFileExtensionFromContentType(String contentType) {
        if (contentType == null) {
            return null;
        }
        
        switch (contentType.toLowerCase()) {
            case "application/pdf":
                return "pdf";
            case "image/jpeg":
            case "image/jpg":
                return "jpg";
            case "image/png":
                return "png";
            case "image/gif":
                return "gif";
            case "text/plain":
                return "txt";
            case "text/csv":
                return "csv";
            case "application/json":
                return "json";
            case "application/xml":
                return "xml";
            case "application/msword":
                return "doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                return "docx";
            case "application/vnd.ms-excel":
                return "xls";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":
                return "xlsx";
            case "application/vnd.ms-powerpoint":
                return "ppt";
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation":
                return "pptx";
            default:
                return null;
        }
    }
    
    /**
     * 获取技能描述
     * @param skillName 技能名称
     * @return 技能描述
     */
    @GetMapping("/description/{skillName}")
    @Operation(summary = "获取技能描述", description = "获取指定技能的描述信息")
    public Result<String> getSkillDescription(@PathVariable String skillName) {
        String description = skillExecutorService.getSkillDescription(skillName);
        return Result.success(description);
    }
    
    @PostMapping(value = "/html-ppt/generate")
    @Operation(summary = "生成HTML PPT（返回JSON）", description = "生成HTML PPT，支持选择不同AI模型：qwen3.6-plus, glm-5, kimi-k2.5, MiniMax-M2.5")
    public Result<java.util.Map<String, Object>> generateHtmlPpt(@RequestBody HtmlPptRequest request) {
        String outline = request.getOutline();
        String theme = request.getTheme();
        String title = request.getTitle();
        String model = request.getModel();
        
        try {
            Object result = skillExecutorService.executeSkill("HTML PPT Skill", outline, theme, title, model);
            if (result instanceof java.util.Map) {
                java.util.Map<?, ?> map = (java.util.Map<?, ?>) result;
                String htmlContent = (String) map.get("htmlContent");
                if (htmlContent != null) {
                    String fileName = (title != null && !title.isEmpty()) ? 
                        title.replaceAll("[^a-zA-Z0-9\u4e00-\u9fa5]", "_") + ".html" : 
                        "presentation.html";
                    
                    java.util.Map<String, Object> data = new java.util.HashMap<>();
                    data.put("htmlContent", htmlContent);
                    data.put("fileName", fileName);
                    data.put("title", title);
                    data.put("theme", theme);
                    data.put("model", model);
                    data.put("success", true);
                    
                    return Result.success(data);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("生成失败：" + e.getMessage());
        }
        return Result.fail("生成失败");
    }
    
    @PostMapping(value = "/html-ppt/view", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "生成并在浏览器中查看HTML PPT", description = "生成HTML并在浏览器中直接打开查看，支持选择不同AI模型")
    public String viewHtmlPpt(@RequestBody HtmlPptRequest request) {
        String outline = request.getOutline();
        String theme = request.getTheme();
        String title = request.getTitle();
        String model = request.getModel();
        
        Object result = skillExecutorService.executeSkill("HTML PPT Skill", outline, theme, title, model);
        if (result instanceof java.util.Map) {
            java.util.Map<?, ?> map = (java.util.Map<?, ?>) result;
            String htmlContent = (String) map.get("htmlContent");
            if (htmlContent != null) {
                return htmlContent;
            }
        }
        return "<h1>生成失败！</h1>";
    }
    
    @PostMapping(value = "/html-ppt/download")
    @Operation(summary = "生成并下载HTML PPT文件", description = "生成HTML PPT并直接下载到本地，支持选择不同AI模型")
    public ResponseEntity<org.springframework.core.io.Resource> downloadHtmlPpt(@RequestBody HtmlPptRequest request) {
        String outline = request.getOutline();
        String theme = request.getTheme();
        String title = request.getTitle();
        String model = request.getModel();
        
        try {
            Object result = skillExecutorService.executeSkill("HTML PPT Skill", outline, theme, title, model);
            if (result instanceof java.util.Map) {
                java.util.Map<?, ?> map = (java.util.Map<?, ?>) result;
                String htmlContent = (String) map.get("htmlContent");
                if (htmlContent != null) {
                    byte[] htmlBytes = htmlContent.getBytes(StandardCharsets.UTF_8);
                    ByteArrayResource resource = new ByteArrayResource(htmlBytes);
                    
                    String fileName = (title != null && !title.isEmpty()) ? 
                        title.replaceAll("[^a-zA-Z0-9\u4e00-\u9fa5]", "_") + ".html" : 
                        "presentation.html";
                    
                    HttpHeaders headers = new HttpHeaders();
                    String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
                    headers.add(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);
                    headers.setContentType(MediaType.TEXT_HTML);
                    headers.setContentLength(htmlBytes.length);
                    headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
                    headers.add(HttpHeaders.PRAGMA, "no-cache");
                    headers.add(HttpHeaders.EXPIRES, "0");
                    
                    return ResponseEntity.ok()
                        .headers(headers)
                        .body(resource);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.internalServerError().build();
    }
}
