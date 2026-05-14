package com.javaee.documentservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.javaee.documentservice.entity.DocumentVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文档版本Mapper接口
 */
@Mapper
public interface DocumentVersionMapper extends BaseMapper<DocumentVersion> {

    List<DocumentVersion> selectByDocumentId(@Param("documentId") String documentId);

    DocumentVersion selectLatestVersion(@Param("documentId") String documentId);

    Integer selectMaxVersionNumber(@Param("documentId") String documentId);

    DocumentVersion selectByDocumentIdAndVersion(@Param("documentId") String documentId, @Param("versionNumber") Integer versionNumber);
}
