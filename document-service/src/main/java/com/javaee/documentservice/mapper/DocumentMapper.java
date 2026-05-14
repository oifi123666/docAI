package com.javaee.documentservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.javaee.documentservice.entity.Document;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文档Mapper接口
 */
@Mapper
public interface DocumentMapper extends BaseMapper<Document> {

    List<Document> selectByUserId(@Param("userId") Long userId);

    List<Document> selectByCategory(@Param("category") String category);

    List<Document> searchByKeyword(@Param("keyword") String keyword);

    List<Document> selectByStatus(@Param("status") String status);
}
