package com.javaee.aiservice.agent;

/**
 * 知识索引任务状态机。
 */
public enum KnowledgeIndexStatus {
    PENDING,
    PARSING,
    EMBEDDING,
    INDEXED,
    FAILED
}
