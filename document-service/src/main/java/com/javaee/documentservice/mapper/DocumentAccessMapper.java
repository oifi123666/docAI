package com.javaee.documentservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.javaee.documentservice.entity.DocumentAccess;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文档协作访问关系 Mapper。
 */
@Mapper
public interface DocumentAccessMapper extends BaseMapper<DocumentAccess> {
}
