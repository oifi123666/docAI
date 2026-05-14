package com.javaee.documentservice.service.impl;

import com.javaee.common.exception.BusinessException;
import com.javaee.documentservice.dto.AnnotationCreateDTO;
import com.javaee.documentservice.entity.Document;
import com.javaee.documentservice.entity.DocumentAnnotation;
import com.javaee.documentservice.mapper.DocumentAnnotationMapper;
import com.javaee.documentservice.mapper.DocumentMapper;
import com.javaee.documentservice.service.AnnotationService;
import com.javaee.documentservice.vo.AnnotationVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnnotationServiceImpl implements AnnotationService {

    private static final Logger log = LoggerFactory.getLogger(AnnotationServiceImpl.class);

    @Autowired
    private DocumentAnnotationMapper annotationMapper;

    @Autowired
    private DocumentMapper documentMapper;

    @Override
    @Transactional
    public AnnotationVO createAnnotation(AnnotationCreateDTO dto, Long userId) {
        Document document = documentMapper.selectById(dto.getDocumentId());
        if (document == null) {
            throw new BusinessException("文档不存在");
        }

        DocumentAnnotation annotation = new DocumentAnnotation();
        annotation.setDocumentId(dto.getDocumentId());
        annotation.setUserId(userId);
        annotation.setLineNumber(dto.getLineNumber());
        annotation.setStartOffset(dto.getStartOffset());
        annotation.setEndOffset(dto.getEndOffset());
        annotation.setAnnotationType(dto.getAnnotationType());
        annotation.setContent(dto.getContent());
        annotation.setColor(dto.getColor());
        annotation.setStatus("active");
        annotation.setCreatedBy(String.valueOf(userId));
        annotation.setCreateTime(LocalDateTime.now());
        annotation.setUpdateTime(LocalDateTime.now());

        annotationMapper.insert(annotation);
        log.info("创建批注成功: id={}, documentId={}", annotation.getId(), dto.getDocumentId());

        return convertToVO(annotation);
    }

    @Override
    public List<AnnotationVO> getAnnotationsByDocumentId(String documentId) {
        List<DocumentAnnotation> annotations = annotationMapper.selectByDocumentId(documentId);
        return annotations.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public AnnotationVO getAnnotationById(String id) {
        DocumentAnnotation annotation = annotationMapper.selectById(id);
        if (annotation == null) {
            throw new BusinessException("批注不存在");
        }
        return convertToVO(annotation);
    }

    @Override
    @Transactional
    public void updateAnnotation(String id, AnnotationCreateDTO dto, Long userId) {
        DocumentAnnotation annotation = annotationMapper.selectById(id);
        if (annotation == null) {
            throw new BusinessException("批注不存在");
        }

        if (!annotation.getUserId().equals(userId)) {
            throw new BusinessException("无权修改此批注");
        }

        annotation.setContent(dto.getContent());
        annotation.setAnnotationType(dto.getAnnotationType());
        annotation.setColor(dto.getColor());
        annotation.setUpdateTime(LocalDateTime.now());

        annotationMapper.updateById(annotation);
        log.info("更新批注成功: id={}", id);
    }

    @Override
    @Transactional
    public void deleteAnnotation(String id, Long userId) {
        DocumentAnnotation annotation = annotationMapper.selectById(id);
        if (annotation == null) {
            throw new BusinessException("批注不存在");
        }

        if (!annotation.getUserId().equals(userId)) {
            throw new BusinessException("无权删除此批注");
        }

        annotation.setStatus("deleted");
        annotationMapper.updateById(annotation);
        log.info("删除批注成功: id={}", id);
    }

    @Override
    public List<AnnotationVO> getAnnotationsByLineNumber(String documentId, Integer lineNumber) {
        List<DocumentAnnotation> annotations = annotationMapper.selectByLineNumber(documentId, lineNumber);
        return annotations.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    private AnnotationVO convertToVO(DocumentAnnotation annotation) {
        AnnotationVO vo = new AnnotationVO();
        vo.setId(annotation.getId());
        vo.setDocumentId(annotation.getDocumentId());
        vo.setUserId(annotation.getUserId());
        vo.setLineNumber(annotation.getLineNumber());
        vo.setStartOffset(annotation.getStartOffset());
        vo.setEndOffset(annotation.getEndOffset());
        vo.setAnnotationType(annotation.getAnnotationType());
        vo.setContent(annotation.getContent());
        vo.setColor(annotation.getColor());
        vo.setStatus(annotation.getStatus());
        vo.setCreatedBy(annotation.getCreatedBy());
        vo.setCreateTime(annotation.getCreateTime());
        vo.setUpdateTime(annotation.getUpdateTime());
        return vo;
    }
}
