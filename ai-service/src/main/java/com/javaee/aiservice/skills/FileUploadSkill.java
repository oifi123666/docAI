package com.javaee.aiservice.skills;

import com.javaee.aiservice.security.BucketPermissionService;
import com.javaee.aiservice.security.RequestUserContext;
import com.javaee.aiservice.service.MinIOService;
import com.javaee.common.utils.UserBucketUtils;
import org.springframework.web.multipart.MultipartFile;

public class FileUploadSkill implements Skill {

    private final MinIOService minIOService;
    private final BucketPermissionService bucketPermissionService;
    private final RequestUserContext requestUserContext;

    public FileUploadSkill(MinIOService minIOService, BucketPermissionService bucketPermissionService,
                           RequestUserContext requestUserContext) {
        this.minIOService = minIOService;
        this.bucketPermissionService = bucketPermissionService;
        this.requestUserContext = requestUserContext;
    }

    @Override
    public String getName() {
        return "File Upload Skill";
    }

    @Override
    public String getDescription() {
        return "上传文件到MinIO服务器";
    }

    @Override
    public Object execute(Object... parameters) {
        if (parameters.length < 1) {
            throw new IllegalArgumentException("至少需要提供文件参数");
        }

        MultipartFile file = (MultipartFile) parameters[0];
        String bucketName = parameters.length > 1 && parameters[1] != null
                ? (String) parameters[1]
                : UserBucketUtils.bucketNameForUser(requestUserContext.getRequiredUserId());
        String objectName = parameters.length > 2 && parameters[2] != null ? (String) parameters[2] : null;

        try {
            bucketPermissionService.assertCanAccess(bucketName);
            return minIOService.uploadFile(file, bucketName, objectName);
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }
}
