package com.javaee.aiservice.skills;

import com.javaee.aiservice.service.MinIOService;

import java.io.InputStream;

public class FileDownloadSkill implements Skill {

    private final MinIOService minIOService;

    public FileDownloadSkill(MinIOService minIOService) {
        this.minIOService = minIOService;
    }

    @Override
    public String getName() {
        return "File Download Skill";
    }

    @Override
    public String getDescription() {
        return "从MinIO服务器下载文件";
    }

    @Override
    public Object execute(Object... parameters) {
        if (parameters.length < 1) {
            throw new IllegalArgumentException("至少需要提供对象名称参数");
        }

        String objectName = (String) parameters[0];
        String requestedBucketName = parameters.length > 1 && parameters[1] != null ? (String) parameters[1] : "documents";
        String bucketName = requestedBucketName;
        String bucketMessage = null;

        System.out.println("开始下载文件: bucket=" + requestedBucketName + ", object=" + objectName);

        try {
            // 检查请求的桶是否存在
            if (!minIOService.bucketExists(requestedBucketName)) {
                bucketMessage = "桶不存在: " + requestedBucketName + "，使用默认桶: documents";
                bucketName = "documents";
                System.out.println(bucketMessage);
            }
            
            // 检查默认桶是否存在，如果不存在则使用doc-ai
            if ("documents".equals(bucketName) && !minIOService.bucketExists(bucketName)) {
                bucketMessage = "默认桶不存在: documents，使用备用桶: doc-ai";
                bucketName = "doc-ai";
                System.out.println(bucketMessage);
            }

            // 获取文件元数据
            System.out.println("从桶 " + bucketName + " 获取文件元数据...");
            io.minio.StatObjectResponse metadata = minIOService.getFileMetadata(bucketName, objectName);
            System.out.println("获取文件元数据成功");
            
            // 获取文件内容类型
            String contentType = metadata.contentType();
            System.out.println("文件内容类型: " + contentType);
            
            // 获取文件输入流
            System.out.println("从桶 " + bucketName + " 获取文件输入流...");
            InputStream inputStream = minIOService.downloadFile(bucketName, objectName);
            System.out.println("获取文件输入流成功");
            
            // 返回包含文件流、元数据、桶消息和实际桶名称的数组
            System.out.println("返回文件流和元数据");
            return new Object[] { inputStream, contentType, objectName, bucketMessage, bucketName };
        } catch (Exception e) {
            System.out.println("下载文件失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("下载文件失败: " + e.getMessage(), e);
        }
    }
}
