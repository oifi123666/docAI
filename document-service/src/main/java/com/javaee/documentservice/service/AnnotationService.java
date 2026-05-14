package com.javaee.documentservice.service;

import com.javaee.documentservice.dto.AnnotationCreateDTO;
import com.javaee.documentservice.vo.AnnotationVO;

import java.util.List;

public interface AnnotationService {

    AnnotationVO createAnnotation(AnnotationCreateDTO dto, Long userId);

    List<AnnotationVO> getAnnotationsByDocumentId(String documentId);

    AnnotationVO getAnnotationById(String id);

    void updateAnnotation(String id, AnnotationCreateDTO dto, Long userId);

    void deleteAnnotation(String id, Long userId);

    List<AnnotationVO> getAnnotationsByLineNumber(String documentId, Integer lineNumber);
}
