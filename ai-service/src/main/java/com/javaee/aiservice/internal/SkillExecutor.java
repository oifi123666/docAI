package com.javaee.aiservice.internal;

import com.javaee.aiservice.dto.FileDeleteDTO;
import com.javaee.aiservice.dto.FileDownloadDTO;
import com.javaee.aiservice.service.FileDeleteService;
import com.javaee.aiservice.service.FileDownloadService;
import com.javaee.aiservice.vo.FileDeleteVO;
import com.javaee.aiservice.vo.FileDownloadVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 技能执行器
 * 执行各种技能操作
 * 作为InternalService的技能调用层
 */
@Component
public class SkillExecutor {

    private static final Logger log = LoggerFactory.getLogger(SkillExecutor.class);

    @Autowired
    private InternalService internalService;

    @Autowired
    private FileDeleteService fileDeleteService;

    @Autowired
    private FileDownloadService fileDownloadService;

    /**
     * 执行技能
     * @param skillName 技能名称
     * @param params 参数
     * @return 执行结果
     */
    public Map<String, Object> execute(String skillName, Map<String, Object> params) {
        log.info("执行技能: skillName={}, params={}", skillName, params);

        try {
            Map<String, Object> result;

            switch (skillName) {
                case "file-delete":
                    result = executeFileDelete(params);
                    break;
                case "file-download":
                    result = executeFileDownload(params);
                    break;
                default:
                    result = Map.of(
                        "status", "error",
                        "message", "未知技能: " + skillName
                    );
            }

            internalService.logAudit(skillName, params, result);

            return result;

        } catch (Exception e) {
            log.error("技能执行失败", e);
            Map<String, Object> errorResult = Map.of(
                "status", "error",
                "message", e.getMessage()
            );
            internalService.logAudit(skillName, params, errorResult);
            return errorResult;
        }
    }

    /**
     * 执行文件删除
     */
    private Map<String, Object> executeFileDelete(Map<String, Object> params) {
        FileDeleteDTO dto = new FileDeleteDTO();
        dto.setBucketName((String) params.get("bucketName"));
        dto.setObjectName((String) params.get("objectName"));
        dto.setRequireConfirmation((Boolean) params.get("requireConfirmation"));
        dto.setConfirmationToken((String) params.get("confirmationToken"));

        FileDeleteVO vo = fileDeleteService.deleteFile(dto, "system");
        
        Map<String, Object> result = new HashMap<>();
        result.put("status", vo.getStatus());
        result.put("confirmationToken", vo.getConfirmationToken());
        result.put("confirmationExpiry", vo.getConfirmationExpiry());
        result.put("recycleId", vo.getRecycleId());
        result.put("message", vo.getMessage());
        
        return result;
    }

    /**
     * 执行文件下载（获取URL）
     */
    private Map<String, Object> executeFileDownload(Map<String, Object> params) {
        FileDownloadDTO dto = new FileDownloadDTO();
        dto.setBucketName((String) params.get("bucketName"));
        dto.setObjectName((String) params.get("objectName"));

        FileDownloadVO vo = fileDownloadService.getFileUrl(dto);
        
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("fileUrl", vo.getFileUrl());
        result.put("bucketName", vo.getBucketName());
        result.put("objectName", vo.getObjectName());
        result.put("expirySeconds", vo.getExpirySeconds());
        
        return result;
    }

    /**
     * 获取所有可用技能
     * @return 技能列表
     */
    public Map<String, Map<String, Object>> listSkills() {
        Map<String, Map<String, Object>> skills = new HashMap<>();
        
        skills.put("file-delete", internalService.getSkillDescription("file-delete"));
        skills.put("file-download", internalService.getSkillDescription("file-download"));
        
        return skills;
    }
}
