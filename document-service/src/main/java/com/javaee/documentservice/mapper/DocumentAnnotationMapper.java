package com.javaee.documentservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.javaee.documentservice.entity.DocumentAnnotation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DocumentAnnotationMapper extends BaseMapper<DocumentAnnotation> {

    List<DocumentAnnotation> selectByDocumentId(@Param("documentId") String documentId);

    List<DocumentAnnotation> selectByLineNumber(@Param("documentId") String documentId, @Param("lineNumber") Integer lineNumber);
}
