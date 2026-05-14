package com.javaee.documentservice.service.impl;

import com.javaee.common.exception.BusinessException;
import com.javaee.documentservice.client.FileServiceClient;
import com.javaee.documentservice.dto.DocumentCreateDTO;
import com.javaee.documentservice.dto.DocumentQueryDTO;
import com.javaee.documentservice.dto.DocumentUpdateDTO;
import com.javaee.documentservice.entity.Document;
import com.javaee.documentservice.entity.DocumentVersion;
import com.javaee.documentservice.mapper.DocumentMapper;
import com.javaee.documentservice.mapper.DocumentVersionMapper;
import com.javaee.documentservice.service.DocumentContentService;
import com.javaee.documentservice.service.DocumentService;
import com.javaee.documentservice.util.DocumentParserUtil;
import com.javaee.documentservice.vo.DocumentVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档服务实现类
 * 实现文档的CRUD操作和版本控制功能
 * 文档内容存储到MinIO，MySQL只存储元数据和引用
 */
@Service
public class DocumentServiceImpl implements DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentServiceImpl.class);

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private DocumentVersionMapper documentVersionMapper;

    @Autowired
    private DocumentContentService documentContentService;

    @Autowired
    private FileServiceClient fileServiceClient;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 创建文档
     * 创建新文档并保存初始版本
     * 通过fileId从file-service获取文件内容，使用Apache POI解析
     * @param dto 创建文档请求参数
     * @param userId 创建用户ID
     * @return 文档VO
     */
    @Override
    @Transactional
    public DocumentVO create(DocumentCreateDTO dto, Long userId) {
        Document document = new Document();
        document.setTitle(dto.getTitle());
        document.setFileId(dto.getFileId());
        document.setCategory(dto.getCategory());
        document.setTags(convertListToJson(dto.getTags()));
        document.setUserId(userId);
        document.setStatus("active");
        document.setVersion(1);
        document.setCreatedBy(String.valueOf(userId));
        document.setCreateTime(LocalDateTime.now());
        document.setUpdateTime(LocalDateTime.now());

        documentMapper.insert(document);

        // 通过fileId从file-service获取文件内容并解析
        String content = "";
        String fileName = "";
        if (dto.getFileId() != null && !dto.getFileId().isEmpty()) {
            try {
                // 获取文件名
                ResponseEntity<String> fileNameResponse = fileServiceClient.getFileName(dto.getFileId());
                fileName = fileNameResponse.getBody() != null ? fileNameResponse.getBody() : "unknown";
                
                // 下载文件内容
                ResponseEntity<byte[]> fileResponse = fileServiceClient.downloadFile(dto.getFileId());
                byte[] fileContent = fileResponse.getBody();
                
                if (fileContent != null && fileContent.length > 0) {
                    // 使用Apache POI解析文档
                    content = DocumentParserUtil.parseDocument(fileContent, fileName);
                    log.info("文档解析成功: fileId={}, fileName={}, contentLength={}", 
                            dto.getFileId(), fileName, content.length());
                }
            } catch (Exception e) {
                log.error("从file-service获取文件失败: fileId={}", dto.getFileId(), e);
                throw new BusinessException("获取文件内容失败: " + e.getMessage());
            }
        }

        // 将解析后的内容存储到MinIO
        if (content != null && !content.isEmpty()) {
            documentContentService.saveContent(document.getId(), content);
            
            // 设置文档摘要和关键词
            document.setSummary(DocumentParserUtil.getSummary(content, 200));
            documentMapper.updateById(document);
        }

        saveVersion(document, "初始版本", content);

        DocumentVO vo = convertToVO(document);
        vo.setContent(content);
        return vo;
    }

    /**
     * 更新文档
     * 更新文档内容前先保存当前版本作为历史版本
     * @param id 文档ID
     * @param dto 更新文档请求参数
     * @param userId 更新用户ID
     * @return 更新后的文档VO
     */
    @Override
    @Transactional
    public DocumentVO update(String id, DocumentUpdateDTO dto, Long userId) {
        Document document = documentMapper.selectById(id);
        if (document == null) {
            throw new BusinessException("文档不存在");
        }

        // 获取当前内容用于保存版本
        String currentContent = documentContentService.getContent(id);

        saveVersion(document, dto.getChangeLog(), currentContent);

        document.setTitle(dto.getTitle());
        document.setCategory(dto.getCategory());
        document.setTags(convertListToJson(dto.getTags()));
        document.setVersion(document.getVersion() + 1);
        document.setUpdateTime(LocalDateTime.now());

        documentMapper.updateById(document);

        // 更新MinIO中的文档内容
        if (dto.getContent() != null && !dto.getContent().isEmpty()) {
            documentContentService.updateContent(id, dto.getContent());
            
            // 更新摘要
            document.setSummary(DocumentParserUtil.getSummary(dto.getContent(), 200));
            documentMapper.updateById(document);
        }

        DocumentVO vo = convertToVO(document);
        vo.setContent(dto.getContent());
        return vo;
    }

    /**
     * 删除文档（软删除）
     * 将文档状态标记为已删除，不物理删除
     * @param id 文档ID
     * @param userId 删除用户ID
     */
    @Override
    @Transactional
    public void delete(String id, Long userId) {
        Document document = documentMapper.selectById(id);
        if (document == null) {
            throw new BusinessException("文档不存在");
        }

        document.setStatus("deleted");
        document.setUpdateTime(LocalDateTime.now());
        documentMapper.updateById(document);

        // 删除MinIO中的文档内容
        documentContentService.deleteContent(id);
    }

    /**
     * 根据ID获取文档详情
     * 从MySQL获取元数据，从MinIO获取内容
     * @param id 文档ID
     * @return 文档VO
     */
    @Override
    public DocumentVO getById(String id) {
        Document document = documentMapper.selectById(id);
        if (document == null) {
            throw new BusinessException("文档不存在");
        }
        DocumentVO vo = convertToVO(document);
        
        // 从MinIO获取文档内容
        String content = documentContentService.getContent(id);
        vo.setContent(content);
        
        return vo;
    }

    /**
     * 获取用户的文档列表
     * @param userId 用户ID
     * @return 文档VO列表（不包含内容，如需内容需单独调用getById）
     */
    @Override
    public List<DocumentVO> getByUserId(Long userId) {
        List<Document> documents = documentMapper.selectByUserId(userId);
        return documents.stream().map(this::convertToVOWithoutContent).collect(Collectors.toList());
    }

    /**
     * 搜索文档
     * 支持按关键词搜索（标题、分类）或按分类搜索
     * @param dto 查询参数
     * @return 文档VO列表（不包含内容，如需内容需单独调用getById）
     */
    @Override
    public List<DocumentVO> search(DocumentQueryDTO dto) {
        List<Document> documents;

        if (dto.getKeyword() != null && !dto.getKeyword().isEmpty()) {
            documents = documentMapper.searchByKeyword(dto.getKeyword());
        } else if (dto.getCategory() != null && !dto.getCategory().isEmpty()) {
            documents = documentMapper.selectByCategory(dto.getCategory());
        } else {
            documents = documentMapper.selectByStatus("active");
        }

        return documents.stream().map(this::convertToVOWithoutContent).collect(Collectors.toList());
    }

    /**
     * 获取文档的所有版本
     * @param documentId 文档ID
     * @return 文档版本列表（按版本号降序）
     */
    @Override
    public List<DocumentVersion> getVersions(String documentId) {
        return documentVersionMapper.selectByDocumentId(documentId);
    }

    /**
     * 恢复文档到指定版本
     * 先保存当前版本作为历史版本，再恢复到指定版本的内容
     * @param documentId 文档ID
     * @param versionNumber 版本号
     * @param userId 操作用户ID
     * @return 恢复后的文档VO
     */
    @Override
    @Transactional
    public DocumentVO restoreVersion(String documentId, Integer versionNumber, Long userId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException("文档不存在");
        }

        DocumentVersion version = documentVersionMapper.selectByDocumentIdAndVersion(documentId, versionNumber);
        if (version == null) {
            throw new BusinessException("版本不存在");
        }

        // 获取当前内容用于保存版本
        String currentContent = documentContentService.getContent(documentId);
        saveVersion(document, "恢复到版本" + versionNumber, currentContent);

        document.setTitle(version.getTitle());
        document.setSummary(version.getSummary());
        document.setKeywords(version.getKeywords());
        document.setVersion(document.getVersion() + 1);
        document.setUpdateTime(LocalDateTime.now());

        documentMapper.updateById(document);

        // 恢复MinIO中的文档内容
        documentContentService.updateContent(documentId, version.getContent());

        DocumentVO vo = convertToVO(document);
        // 从MinIO获取恢复后的内容
        vo.setContent(version.getContent());
        return vo;
    }

    /**
     * 保存文档版本
     * @param document 当前文档
     * @param changeLog 变更日志
     * @param content 文档内容（从MinIO获取）
     */
    private void saveVersion(Document document, String changeLog, String content) {
        DocumentVersion version = new DocumentVersion();
        version.setDocumentId(document.getId());
        version.setVersionNumber(document.getVersion());
        version.setTitle(document.getTitle());
        version.setContent(content);
        version.setSummary(document.getSummary());
        version.setKeywords(document.getKeywords());
        version.setChangeLog(changeLog);
        version.setCreatedBy(document.getCreatedBy());
        version.setCreateTime(LocalDateTime.now());

        documentVersionMapper.insert(version);
    }

    /**
     * 将Document实体转换为DocumentVO（包含内容）
     * @param document 文档实体
     * @return 文档VO
     */
    private DocumentVO convertToVO(Document document) {
        DocumentVO vo = new DocumentVO();
        vo.setId(document.getId());
        vo.setTitle(document.getTitle());
        vo.setSummary(document.getSummary());
        vo.setKeywords(convertJsonToList(document.getKeywords()));
        vo.setFileId(document.getFileId());
        vo.setCategory(document.getCategory());
        vo.setTags(convertJsonToList(document.getTags()));
        vo.setVersion(document.getVersion());
        vo.setStatus(document.getStatus());
        vo.setCreatedBy(document.getCreatedBy());
        vo.setCreateTime(document.getCreateTime());
        vo.setUpdateTime(document.getUpdateTime());
        return vo;
    }

    /**
     * 将Document实体转换为DocumentVO（不包含内容，用于列表查询）
     * @param document 文档实体
     * @return 文档VO
     */
    private DocumentVO convertToVOWithoutContent(Document document) {
        return convertToVO(document);
    }

    /**
     * 将List转换为JSON字符串
     * @param list 字符串列表
     * @return JSON字符串
     */
    private String convertListToJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    /**
     * 将JSON字符串转换为List
     * @param json JSON字符串
     * @return 字符串列表
     */
    private List<String> convertJsonToList(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return Arrays.asList(objectMapper.readValue(json, String[].class));
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}