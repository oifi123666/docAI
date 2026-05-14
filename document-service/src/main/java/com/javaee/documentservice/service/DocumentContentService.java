package com.javaee.documentservice.service;

/**
 * 文档内容服务接口
 * 负责文档内容的存储和读取（存储到MinIO）
 */
public interface DocumentContentService {

    /**
     * 保存文档内容到MinIO
     * @param documentId 文档ID
     * @param content 文档内容
     * @return 是否成功
     */
    boolean saveContent(String documentId, String content);

    /**
     * 从MinIO读取文档内容
     * @param documentId 文档ID
     * @return 文档内容，如果不存在返回null
     */
    String getContent(String documentId);

    /**
     * 删除MinIO中的文档内容
     * @param documentId 文档ID
     * @return 是否成功
     */
    boolean deleteContent(String documentId);

    /**
     * 更新MinIO中的文档内容
     * @param documentId 文档ID
     * @param content 新的文档内容
     * @return 是否成功
     */
    boolean updateContent(String documentId, String content);
}
