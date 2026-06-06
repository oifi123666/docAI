package com.javaee.aiservice.skills;

import com.javaee.aiservice.security.BucketPermissionService;
import com.javaee.aiservice.security.RequestUserContext;
import com.javaee.aiservice.service.MinIOService;
import com.javaee.common.utils.UserBucketUtils;

import java.io.InputStream;

public class FileDownloadSkill implements Skill {

    private final MinIOService minIOService;
    private final BucketPermissionService bucketPermissionService;
    private final RequestUserContext requestUserContext;

    public FileDownloadSkill(MinIOService minIOService, BucketPermissionService bucketPermissionService,
                             RequestUserContext requestUserContext) {
        this.minIOService = minIOService;
        this.bucketPermissionService = bucketPermissionService;
        this.requestUserContext = requestUserContext;
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
        String requestedBucketName = parameters.length > 1 && parameters[1] != null
                ? (String) parameters[1]
                : UserBucketUtils.bucketNameForUser(requestUserContext.getRequiredUserId());
        String bucketName = requestedBucketName;
        String bucketMessage = null;

        System.out.println("开始下载文件: bucket=" + requestedBucketName + ", object=" + objectName);

        try {
            // 检查请求的桶是否存在
            if (!minIOService.bucketExists(requestedBucketName)) {
                bucketMessage = "桶不存在: " + requestedBucketName;
                System.out.println(bucketMessage);
            }
            if (bucketMessage != null) {
                throw new IllegalArgumentException(bucketMessage);
            }
            bucketPermissionService.assertCanAccess(bucketName);

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
