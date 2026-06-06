package com.javaee.aiservice.agent.execution.tool;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Central catalog for every tool the Agent planner is allowed to use.
 */
@Component
public class AgentToolRegistry {

    private final Map<String, AgentToolDefinition> tools = new LinkedHashMap<>();

    public AgentToolRegistry() {
        register("direct-answer", "不调用外部工具，直接基于模型和上下文回答用户。",
                Map.of("question", "用户问题"), Set.of(), false, "llm", false);
        register("ask-user", "当任务参数缺失或存在歧义时，向用户提出澄清问题。不要在已经掌握所需信息时使用。",
                Map.of("question", "需要向用户澄清的问题",
                        "missingFields", "本次缺失的字段名，逗号分隔，可选",
                        "options", "可供用户选择的候选项，分号分隔，可选"),
                Set.of("question"), false, "interaction", true);
        register("rag-answer", "检索知识库并调用模型生成最终问答答案。",
                Map.of("question", "用户问题", "topK", "检索条数，默认3", "strategy", "重排序策略，默认HYBRID"),
                Set.of("question"), false, "rag", false);
        register("rag-search", "只检索知识库，返回相关片段和来源，不生成答案。",
                Map.of("query", "查询词", "topK", "检索条数，默认5", "strategy", "重排序策略，默认HYBRID"),
                Set.of("query"), false, "rag", false);
        register("text-summarize", "对文本进行摘要。",
                Map.of("content", "待摘要文本", "maxLength", "摘要最大长度，默认300", "model", "可选模型代码"),
                Set.of("content"), false, "text", false);
        register("text-analyze", "统计文本字符、中文、英文、数字、空格、标点、行数。",
                Map.of("content", "待分析文本"), Set.of("content"), false, "text", false);
        register("keyword-extract", "从文本中提取关键词。",
                Map.of("content", "待提取文本", "count", "关键词数量，默认8", "model", "可选模型代码"),
                Set.of("content"), false, "text", false);
        register("text-correct", "对文本进行纠错、润色或改写。",
                Map.of("content", "待处理文本", "instruction", "纠错/润色/改写要求", "model", "可选模型代码"),
                Set.of("content"), false, "text", false);
        register("file-download-url", "生成文件预签名下载地址。",
                Map.of("bucketName", "存储桶名称，可选", "objectName", "对象名称，必填"),
                Set.of("objectName"), false, "file", false);
        register("file-delete", "永久删除指定文件，不进入回收站。可传前端documentId，由服务端删除对应业务文档；也兼容直接传objectName永久删除MinIO对象。",
                Map.of("bucketName", "存储桶名称，可选",
                        "objectName", "对象名称，与documentId二选一",
                        "documentId", "前端业务文档ID，与objectName二选一"),
                Set.of(), false, "file", false);
        register("file-restore", "从回收站恢复文件。",
                Map.of("recycleId", "回收站记录ID", "bucketName", "存储桶名称，可选", "newObjectName", "新对象名称，可选"),
                Set.of("recycleId"), true, "file", false);
        register("recycle-list", "查看回收站文件列表。",
                Map.of("bucketName", "存储桶名称，可选"), Set.of(), false, "file", false);
        register("file-version-list", "查看文件版本列表。",
                Map.of("bucketName", "存储桶名称，可选", "objectName", "对象名称，必填"),
                Set.of("objectName"), false, "file", false);
        register("file-version-switch", "切换文件当前版本。",
                Map.of("bucketName", "存储桶名称，可选", "objectName", "对象名称，必填",
                        "targetVersionId", "目标版本ID"), Set.of("objectName", "targetVersionId"), true, "file", false);
        register("html-ppt-generate", "根据大纲生成 HTML PPT。",
                Map.of("outline", "PPT大纲", "theme", "主题，默认tokyo-night", "title", "标题", "model", "可选模型代码"),
                Set.of("outline"), false, "generation", false);
        register("document-read", "读取业务文档当前内容。用于在扩写、改写、润色前获取原文档。",
                Map.of("documentId", "业务文档ID，必填"),
                Set.of("documentId"), false, "document", false);
        register("document-write", "将AI生成的文本内容返回给前端，由当前编辑器直接写入页面文档；不在服务端保存文件或刷新MinIO对象。",
                Map.of("documentId", "业务文档ID，可选，用于前端校验当前文档",
                        "content", "要写回文档的完整文本内容，必填",
                        "writeMode", "写入模式: append(追加,默认) / overwrite(覆盖) / replace-selection(替换选中内容) / insert(插入)",
                        "changeLog", "本次前端增量修改说明，默认由Agent生成",
                        "selectionText", "前端当前选中的文本，可选",
                        "insertAfterText", "从当前文档中复制的一小段锚点文本，前端会把内容插入到该段后面，可选",
                        "contentFormat", "内容格式: plain_text(默认) / html"),
                Set.of("content"), false, "document", false);
        register("text-to-file", "兼容旧的写文件工具名：将AI生成文本返回给前端编辑器待写入，不直接调用MinIO。",
                Map.of("content", "要写入的文本内容，必填",
                        "objectName", "旧流程的文件对象名称，可选，仅用于兼容展示",
                        "bucketName", "旧流程的存储桶名称，可选，仅用于兼容展示",
                        "contentType", "内容类型，默认text/plain",
                        "writeMode", "写入模式: append(追加,默认) / overwrite(覆盖) / replace-selection(替换选中内容) / insert(插入)",
                        "changeLog", "本次前端增量修改说明，默认由Agent生成",
                        "insertAfterText", "从当前文档中复制的一小段锚点文本，前端会把内容插入到该段后面，可选",
                        "selectionText", "前端当前选中的文本，可选"),
                Set.of("content"), false, "file", false);
    }

    public AgentToolDefinition get(String name) {
        return tools.get(name);
    }

    public List<AgentToolDefinition> list() {
        return new ArrayList<>(tools.values());
    }

    public boolean contains(String name) {
        return tools.containsKey(name);
    }

    private void register(String name, String description, Map<String, String> parameters, Set<String> requiredParameters,
                          boolean destructive, String category, boolean requiresUserAction) {
        Map<String, AgentToolParameterDefinition> parameterSchema = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String parameterName = entry.getKey();
            parameterSchema.put(parameterName, new AgentToolParameterDefinition(
                    parameterName,
                    inferType(parameterName),
                    entry.getValue(),
                    requiredParameters.contains(parameterName),
                    defaultValue(parameterName),
                    allowedValues(parameterName),
                    minValue(parameterName),
                    maxValue(parameterName),
                    pattern(parameterName)
            ));
        }
        tools.put(name, new AgentToolDefinition(name, description, parameters, parameterSchema,
                destructive, category, requiresUserAction, destructive ? "high" : "low"));
    }

    private List<Object> allowedValues(String parameterName) {
        return switch (parameterName) {
            case "strategy" -> List.of("HYBRID", "VECTOR", "BM25");
            case "writeMode" -> List.of("overwrite", "append", "insert", "replace-selection");
            default -> null;
        };
    }

    private Number minValue(String parameterName) {
        return switch (parameterName) {
            case "topK", "count" -> 1;
            case "maxLength" -> 50;
            default -> null;
        };
    }

    private Number maxValue(String parameterName) {
        return switch (parameterName) {
            case "topK" -> 50;
            case "count" -> 50;
            case "maxLength" -> 5000;
            default -> null;
        };
    }

    private String pattern(String parameterName) {
        return null;
    }

    private String inferType(String parameterName) {
        if (Set.of("topK", "count", "maxLength").contains(parameterName)) {
            return "integer";
        }
        if (Set.of("requireConfirmation", "reindexAfterWrite").contains(parameterName)) {
            return "boolean";
        }
        return "string";
    }

    private Object defaultValue(String parameterName) {
        return switch (parameterName) {
            case "topK" -> 5;
            case "count" -> 8;
            case "maxLength" -> 300;
            case "strategy" -> "HYBRID";
            case "requireConfirmation" -> true;
            case "writeMode" -> "append";
            case "contentType" -> "text/plain";
            case "reindexAfterWrite" -> true;
            default -> null;
        };
    }
}
