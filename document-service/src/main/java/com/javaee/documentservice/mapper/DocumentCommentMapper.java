package com.javaee.documentservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.javaee.documentservice.entity.DocumentComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DocumentCommentMapper extends BaseMapper<DocumentComment> {

    List<DocumentComment> selectByDocumentId(@Param("documentId") String documentId);

    List<DocumentComment> selectByParentId(@Param("parentId") String parentId);

    List<DocumentComment> selectRootCommentsByDocumentId(@Param("documentId") String documentId);
}
