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
    boolean saveContent(String documentId, Long userId, String content);

    /**
     * 保存文档内容到指定MinIO桶。
     */
    boolean saveContent(String documentId, String bucketName, String content);

    /**
     * 从MinIO读取文档内容
     * @param documentId 文档ID
     * @return 文档内容，如果不存在返回null
     */
    String getContent(String documentId, Long userId);

    /**
     * 从指定MinIO桶读取文档内容。
     */
    String getContent(String documentId, String bucketName);

    /**
     * 删除MinIO中的文档内容
     * @param documentId 文档ID
     * @return 是否成功
     */
    boolean deleteContent(String documentId, Long userId);

    /**
     * 删除指定MinIO桶中的文档内容。
     */
    boolean deleteContent(String documentId, String bucketName);

    /**
     * 更新MinIO中的文档内容
     * @param documentId 文档ID
     * @param content 新的文档内容
     * @return 是否成功
     */
    boolean updateContent(String documentId, Long userId, String content);

    /**
     * 更新指定MinIO桶中的文档内容。
     */
    boolean updateContent(String documentId, String bucketName, String content);

    /**
     * 获取文档所在用户桶
     * @param userId 用户ID
     * @return MinIO桶名
     */
    String getBucketName(Long userId);

    /**
     * 获取文档内容对象名
     * @param documentId 文档ID
     * @return MinIO对象名
     */
    String getObjectName(String documentId);
}
