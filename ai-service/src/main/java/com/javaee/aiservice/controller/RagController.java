package com.javaee.aiservice.controller;

import com.javaee.aiservice.agent.ChatService;
import com.javaee.aiservice.rag.DocumentSegmenter;
import com.javaee.aiservice.rag.KnowledgeBase;
import com.javaee.aiservice.rag.Reranker;
import com.javaee.aiservice.rag.VectorStore;
import com.javaee.aiservice.security.RequestUserContext;
import com.javaee.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * RAG控制器
 * 提供知识库相关的REST API接口
 * 支持基础检索、混合检索和重排序功能
 */
@RestController
@RequestMapping("/api/ai/rag")
@Tag(name = "RAG知识库", description = "知识库索引、搜索、问答接口")
public class RagController {

    @Autowired
    private KnowledgeBase knowledgeBase;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private Reranker reranker;

    @Autowired
    private DocumentSegmenter documentSegmenter;

    @Autowired
    private ChatService chatService;

    @Autowired
    private RequestUserContext requestUserContext;

    /**
     * 文档索引
     */
    @PostMapping("/index")
    @Operation(summary = "文档索引", description = "将文档添加到知识库")
    public Result<Void> indexDocument(
            @Parameter(description = "文档ID") @RequestParam String documentId,
            @Parameter(description = "知识库ID") @RequestParam(defaultValue = "default") String knowledgeBaseId,
            @Parameter(description = "文档内容") @RequestBody String content) {
        knowledgeBase.addDocument(documentId, content, userMetadata(knowledgeBaseId));
        return Result.success();
    }

    /**
     * 文档索引（指定分段策略）
     */
    @PostMapping("/index/segment")
    @Operation(summary = "文档索引（指定分段策略）", description = "将文档添加到知识库并指定分段策略")
    public Result<Void> indexDocumentWithSegment(
            @Parameter(description = "文档ID") @RequestParam String documentId,
            @Parameter(description = "文档内容") @RequestBody String content,
            @Parameter(description = "知识库ID") @RequestParam(defaultValue = "default") String knowledgeBaseId,
            @Parameter(description = "分段策略: AUTO, FIXED_LENGTH, CHAPTER, SEMANTIC, HYBRID")
            @RequestParam(defaultValue = "AUTO") String strategy) {

        DocumentSegmenter.StrategyType strategyType;
        try {
            strategyType = DocumentSegmenter.StrategyType.valueOf(strategy.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Result.fail("无效的分段策略: " + strategy);
        }

        knowledgeBase.addDocument(documentId, content, userMetadata(knowledgeBaseId), strategyType);
        return Result.success();
    }

    /**
     * 基础向量检索
     */
    @GetMapping("/search")
    @Operation(summary = "基础检索", description = "使用向量相似度搜索知识库")
    public Result<List<Map<String, Object>>> search(
            @Parameter(description = "查询词") @RequestParam String query,
            @Parameter(description = "返回数量") @RequestParam(defaultValue = "5") int topK,
            @Parameter(description = "知识库ID") @RequestParam(defaultValue = "default") String knowledgeBaseId) {
        List<Map<String, Object>> results = knowledgeBase.search(query, limitTopK(topK),
                DocumentSegmenter.StrategyType.CHAPTER, userMetadata(knowledgeBaseId));
        return Result.success(results);
    }

    /**
     * 混合检索（向量检索 + BM25）
     */
    @GetMapping("/search/hybrid")
    @Operation(summary = "混合检索", description = "使用向量检索和BM25检索的混合方式搜索")
    public Result<List<Map<String, Object>>> hybridSearch(
            @Parameter(description = "查询词") @RequestParam String query,
            @Parameter(description = "返回数量") @RequestParam(defaultValue = "5") int topK,
            @Parameter(description = "知识库ID") @RequestParam(defaultValue = "default") String knowledgeBaseId) {
        List<Map<String, Object>> results = knowledgeBase.hybridSearch(query, limitTopK(topK),
                DocumentSegmenter.StrategyType.CHAPTER, userMetadata(knowledgeBaseId));
        return Result.success(results);
    }

    /**
     * 混合检索加重排序
     */
    @GetMapping("/search/hybrid/rerank")
    @Operation(summary = "混合检索加重排序", description = "混合检索后使用指定策略进行重排序")
    public Result<List<Map<String, Object>>> hybridSearchWithRerank(
            @Parameter(description = "查询词") @RequestParam String query,
            @Parameter(description = "返回数量") @RequestParam(defaultValue = "5") int topK,
            @Parameter(description = "重排序策略: BM25_FUSION, CROSS_ENCODER, HYBRID")
            @RequestParam(defaultValue = "HYBRID") String strategy,
            @Parameter(description = "知识库ID") @RequestParam(defaultValue = "default") String knowledgeBaseId) {

        Reranker.RerankStrategy rerankStrategy;
        try {
            rerankStrategy = Reranker.RerankStrategy.valueOf(strategy.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Result.fail("无效的重排序策略: " + strategy);
        }

        List<Map<String, Object>> results = knowledgeBase.hybridSearchWithRerank(query, limitTopK(topK),
                rerankStrategy, DocumentSegmenter.StrategyType.CHAPTER, userMetadata(knowledgeBaseId));
        return Result.success(results);
    }

    /**
     * 获取支持的重排序策略
     */
    @GetMapping("/rerank/strategies")
    @Operation(summary = "获取重排序策略", description = "获取所有支持的重排序策略")
    public Result<List<String>> getRerankStrategies() {
        List<String> strategies = reranker.getSupportedStrategies();
        return Result.success(strategies);
    }

    /**
     * 知识库问答（使用混合检索加重排序）
     */
    @PostMapping("/query")
    @Operation(summary = "知识库问答", description = "基于知识库进行问答，默认使用混合检索加重排序")
    public Result<Map<String, Object>> query(
            @Parameter(description = "问题") @RequestBody String question,
            @Parameter(description = "重排序策略") @RequestParam(defaultValue = "HYBRID") String strategy,
            @Parameter(description = "知识库ID") @RequestParam(defaultValue = "default") String knowledgeBaseId) {

        Reranker.RerankStrategy rerankStrategy;
        try {
            rerankStrategy = Reranker.RerankStrategy.valueOf(strategy.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Result.fail("无效的重排序策略: " + strategy);
        }

        // 使用混合检索加重排序获取相关文档
        List<Map<String, Object>> results = knowledgeBase.hybridSearchWithRerank(question, 3,
                rerankStrategy, DocumentSegmenter.StrategyType.CHAPTER, userMetadata(knowledgeBaseId));

        StringBuilder context = new StringBuilder();
        for (Map<String, Object> result : results) {
            context.append(result.get("content")).append("\n\n");
        }

        String answerText = generateAnswer(question, context.toString(), strategy);

        Map<String, Object> answer = Map.of(
            "question", question,
            "context", context.toString(),
            "answer", answerText,
            "sources", results.stream().map(r -> r.get("id")).toList(),
            "retrievalStrategy", strategy
        );

        return Result.success(answer);
    }

    /**
     * 获取文档内容
     */
    @GetMapping("/document/{documentId}")
    @Operation(summary = "获取文档内容", description = "获取知识库中的文档内容")
    public Result<String> getDocument(
            @Parameter(description = "文档ID") @PathVariable String documentId) {
        assertDocumentAccess(documentId);
        String content = knowledgeBase.getDocumentContent(documentId);
        return Result.success(content);
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/document/{documentId}")
    @Operation(summary = "删除文档", description = "从知识库删除文档")
    public Result<Void> deleteDocument(
            @Parameter(description = "文档ID") @PathVariable String documentId) {
        assertDocumentAccess(documentId);
        knowledgeBase.removeDocument(documentId);
        return Result.success();
    }

    /**
     * 获取所有文档ID
     */
    @GetMapping("/documents")
    @Operation(summary = "获取文档列表", description = "获取知识库中的所有文档ID")
    public Result<List<String>> getAllDocuments() {
        List<String> documentIds = knowledgeBase.getAllDocumentIds(requestUserContext.getRequiredUserId(), "default");
        return Result.success(documentIds);
    }

    /**
     * 获取文档元数据
     */
    @GetMapping("/document/{documentId}/metadata")
    @Operation(summary = "获取文档元数据", description = "获取文档的元数据信息")
    public Result<Map<String, Object>> getDocumentMetadata(
            @Parameter(description = "文档ID") @PathVariable String documentId) {
        assertDocumentAccess(documentId);
        Map<String, Object> metadata = knowledgeBase.getDocumentMetadata(documentId);
        return Result.success(metadata);
    }

    /**
     * 获取文档分段列表
     */
    @GetMapping("/document/{documentId}/segments")
    @Operation(summary = "获取文档分段", description = "获取文档的分段列表")
    public Result<List<Map<String, Object>>> getDocumentSegments(
            @Parameter(description = "文档ID") @PathVariable String documentId) {
        assertDocumentAccess(documentId);
        List<Map<String, Object>> segments = knowledgeBase.getDocumentSegments(documentId);
        return Result.success(segments);
    }

    /**
     * 获取知识库统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取知识库统计", description = "获取知识库的统计信息")
    public Result<Map<String, Object>> getStatistics(
            @Parameter(description = "知识库ID") @RequestParam(defaultValue = "default") String knowledgeBaseId) {
        Map<String, Object> statistics = knowledgeBase.getStatistics(requestUserContext.getRequiredUserId(), knowledgeBaseId);
        return Result.success(statistics);
    }

    /**
     * 获取支持的分段策略
     */
    @GetMapping("/segment/strategies")
    @Operation(summary = "获取分段策略", description = "获取所有支持的分段策略")
    public Result<Map<String, String>> getSegmentStrategies() {
        Map<String, String> strategies = documentSegmenter.getAvailableStrategies();
        return Result.success(strategies);
    }

    private String generateAnswer(String question, String context, String strategy) {
        if (context == null || context.trim().isEmpty()) {
            return "知识库中未找到相关信息。";
        }

        String prompt = String.format("""
                你是DocAI知识库问答助手。请严格基于【知识库片段】回答用户问题。
                如果片段中没有相关信息，请回答“知识库中未找到相关信息”，不要编造。

                【检索策略】
                %s

                【知识库片段】
                %s

                【用户问题】
                %s

                请给出清晰、准确、结构化的中文回答。
                """, strategy, context, question);

        return chatService.callChatApi(prompt);
    }

    private Map<String, Object> userMetadata(String knowledgeBaseId) {
        return Map.of(
                "userId", requestUserContext.getRequiredUserId(),
                "knowledgeBaseId", knowledgeBaseId
        );
    }

    private int limitTopK(int topK) {
        return Math.max(1, Math.min(topK, 20));
    }

    private void assertDocumentAccess(String documentId) {
        Map<String, Object> metadata = knowledgeBase.getDocumentMetadata(documentId);
        String owner = String.valueOf(metadata.getOrDefault("userId", ""));
        if (!requestUserContext.isAdmin() && !requestUserContext.getRequiredUserId().equals(owner)) {
            throw new SecurityException("无权访问该文档");
        }
    }
}
