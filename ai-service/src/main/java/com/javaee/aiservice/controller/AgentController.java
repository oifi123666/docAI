package com.javaee.aiservice.controller;

import com.javaee.aiservice.agent.KnowledgeIndexAgent;
import com.javaee.aiservice.agent.execution.AgentExecutionService;
import com.javaee.aiservice.agent.execution.approval.AgentApprovalService;
import com.javaee.aiservice.agent.execution.model.AgentExecutionRequest;
import com.javaee.aiservice.agent.execution.task.AgentTaskRegistry;
import com.javaee.aiservice.agent.execution.tool.AgentToolDefinition;
import com.javaee.aiservice.conversation.ConversationManager;
import com.javaee.aiservice.rag.DocumentSegmenter;
import com.javaee.aiservice.rag.KnowledgeBase;
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
 * Agent控制器
 * 提供AI Agent相关的REST API接口
 */
@RestController
@RequestMapping("/api/ai/agent")
@Tag(name = "AI Agent", description = "AI Agent相关接口")
public class AgentController {

    @Autowired
    private KnowledgeIndexAgent knowledgeIndexAgent;

    @Autowired
    private AgentExecutionService agentExecutionService;

    @Autowired
    private AgentApprovalService agentApprovalService;

    @Autowired
    private AgentTaskRegistry agentTaskRegistry;

    @Autowired
    private ConversationManager conversationManager;

    @Autowired
    private RequestUserContext requestUserContext;

    @Autowired
    private KnowledgeBase knowledgeBase;

    /**
     * 统一Agent链路 - 自动规划、工具执行、RAG、最终回答
     */
    @PostMapping("/execute")
    @Operation(summary = "执行统一Agent链路", description = "自动完成任务规划、工具调用、RAG检索、最终回答和对话记忆")
    public Result<Map<String, Object>> executeAgent(@RequestBody AgentExecutionRequest request) {
        request.setUserId(requestUserContext.getRequiredUserId());
        Map<String, Object> result = agentExecutionService.execute(request);
        return Result.success(result);
    }

    /**
     * 统一Agent链路 - 获取工具列表
     */
    @GetMapping("/tools")
    @Operation(summary = "获取Agent工具列表", description = "获取统一Agent可调用的全部工具能力")
    public Result<List<AgentToolDefinition>> listTools() {
        return Result.success(agentExecutionService.listTools());
    }

    /**
     * 统一Agent链路 - 确认危险操作并继续执行
     */
    @PostMapping("/approvals/confirm")
    @Operation(summary = "确认危险操作", description = "携带 agentApprovalToken 与原任务参数继续执行 Agent 任务")
    public Result<Map<String, Object>> confirmApproval(@RequestBody AgentExecutionRequest request) {
        request.setUserId(requestUserContext.getRequiredUserId());
        Map<String, Object> result = agentExecutionService.execute(request);
        return Result.success(result);
    }

    /**
     * 统一Agent链路 - 取消挂起的危险操作
     */
    @PostMapping("/approvals/{token}/cancel")
    @Operation(summary = "取消危险操作", description = "用户拒绝危险操作时丢弃挂起的审批令牌")
    public Result<Boolean> cancelApproval(@PathVariable String token) {
        return Result.success(agentApprovalService.cancel(token));
    }

    /**
     * 任务运行 - 查询单个任务快照。
     */
    @GetMapping("/tasks/{traceId}")
    @Operation(summary = "查询Agent任务快照", description = "返回指定 traceId 的任务执行计划、工具结果与时间线")
    public Result<Map<String, Object>> getTask(@PathVariable String traceId) {
        Map<String, Object> snapshot = agentTaskRegistry.get(traceId);
        if (snapshot == null || snapshot.isEmpty()) {
            return Result.success(Map.of("status", "not_found", "traceId", traceId));
        }
        if (!requestUserContext.isAdmin()
                && !requestUserContext.getRequiredUserId().equals(String.valueOf(snapshot.getOrDefault("userId", "")))) {
            throw new SecurityException("无权查看该任务");
        }
        return Result.success(snapshot);
    }

    /**
     * 任务运行 - 列出当前用户最近的任务。
     */
    @GetMapping("/tasks")
    @Operation(summary = "列出Agent任务历史", description = "按当前用户列出最近的任务快照，管理员可查看全部")
    public Result<List<Map<String, Object>>> listTasks(
            @Parameter(description = "管理员可指定 userId，留空表示当前用户") @RequestParam(required = false) String userId) {
        if (requestUserContext.isAdmin() && userId == null) {
            return Result.success(agentTaskRegistry.listAll());
        }
        String target = requestUserContext.isAdmin() && userId != null ? userId : requestUserContext.getRequiredUserId();
        return Result.success(agentTaskRegistry.listByUser(target));
    }

    /**
     * 任务运行 - 取消挂起的 Agent 任务。
     */
    @PostMapping("/tasks/{traceId}/cancel")
    @Operation(summary = "取消Agent任务", description = "在主循环下一次迭代入口生效；终态任务不会被覆盖")
    public Result<Boolean> cancelTask(@PathVariable String traceId) {
        Map<String, Object> snapshot = agentTaskRegistry.get(traceId);
        if (snapshot != null && !snapshot.isEmpty()
                && !requestUserContext.isAdmin()
                && !requestUserContext.getRequiredUserId().equals(String.valueOf(snapshot.getOrDefault("userId", "")))) {
            throw new SecurityException("无权取消该任务");
        }
        return Result.success(agentTaskRegistry.cancel(traceId));
    }

    /**
     * 任务运行 - 单步重试。
     */
    @PostMapping("/tasks/{traceId}/steps/{stepId}/retry")
    @Operation(summary = "重试Agent任务的指定步骤", description = "在原任务上下文上对指定步骤重新执行工具，可携带 overrideParams 修正参数")
    public Result<Map<String, Object>> retryStep(
            @PathVariable String traceId,
            @PathVariable String stepId,
            @RequestBody(required = false) Map<String, Object> overrideParams) {
        Map<String, Object> snapshot = agentTaskRegistry.get(traceId);
        if (snapshot != null && !snapshot.isEmpty()
                && !requestUserContext.isAdmin()
                && !requestUserContext.getRequiredUserId().equals(String.valueOf(snapshot.getOrDefault("userId", "")))) {
            throw new SecurityException("无权重试该任务");
        }
        return Result.success(agentExecutionService.retryStep(traceId, stepId, overrideParams));
    }

    /**
     * 对话Agent - 开始对话
     */
    @PostMapping("/chat/start")
    @Operation(summary = "开始对话", description = "创建新的对话会话")
    public Result<String> startConversation(
            @Parameter(description = "用户ID") @RequestParam(required = false, defaultValue = "default") String userId) {
        String conversationId = conversationManager.createConversation(requestUserContext.getRequiredUserId());
        return Result.success(conversationId);
    }

    /**
     * 对话Agent - 发送消息
     */
    @PostMapping("/chat")
    @Operation(summary = "发送消息", description = "向统一Agent发送消息，自动完成规划、工具调用和记忆")
    public Result<Map<String, Object>> chat(
            @Parameter(description = "对话ID") @RequestParam String conversationId,
            @Parameter(description = "用户输入") @RequestBody String userInput) {
        AgentExecutionRequest request = new AgentExecutionRequest();
        request.setConversationId(conversationId);
        request.setTask(userInput);
        request.setUserId(requestUserContext.getRequiredUserId());
        request.setContext(Map.of());
        Map<String, Object> result = agentExecutionService.execute(request);
        return Result.success(result);
    }

    /**
     * 对话Agent - 结束对话
     */
    @PostMapping("/chat/{conversationId}/end")
    @Operation(summary = "结束对话", description = "结束指定对话")
    public Result<Boolean> endConversation(
            @Parameter(description = "对话ID") @PathVariable String conversationId) {
        conversationManager.deleteConversationForUser(conversationId, requestUserContext.getRequiredUserId());
        return Result.success(true);
    }

    /**
     * 对话Agent - 获取对话历史
     */
    @GetMapping("/chat/{conversationId}/history")
    @Operation(summary = "获取对话历史", description = "获取对话的消息历史")
    public Result<List<String>> getConversationHistory(
            @Parameter(description = "对话ID") @PathVariable String conversationId) {
        List<String> history = conversationManager.getConversationHistoryForUser(conversationId, requestUserContext.getRequiredUserId());
        return Result.success(history);
    }

    /**
     * 知识索引Agent - 索引文档
     */
    @PostMapping("/knowledge/index")
    @Operation(summary = "索引文档", description = "将文档添加到知识库索引")
    public Result<Map<String, Object>> indexDocument(
            @Parameter(description = "文档ID") @RequestParam String documentId,
            @Parameter(description = "文档内容") @RequestBody String content) {
        Map<String, Object> result = knowledgeIndexAgent.indexDocument(documentId, content, Map.of(
                "userId", requestUserContext.getRequiredUserId(),
                "knowledgeBaseId", "default"
        ));
        return Result.success(result);
    }

    /**
     * 知识索引Agent - 搜索知识库
     */
    @GetMapping("/knowledge/search")
    @Operation(summary = "搜索知识库", description = "搜索知识库中的相关文档")
    public Result<List<Map<String, Object>>> searchKnowledge(
            @Parameter(description = "查询词") @RequestParam String query,
            @Parameter(description = "返回数量") @RequestParam(defaultValue = "5") int topK) {
        List<Map<String, Object>> results = knowledgeBase.search(query, Math.max(1, Math.min(topK, 20)),
                DocumentSegmenter.StrategyType.CHAPTER,
                Map.of("userId", requestUserContext.getRequiredUserId(), "knowledgeBaseId", "default"));
        return Result.success(results);
    }

    /**
     * 知识索引Agent - 删除索引
     */
    @DeleteMapping("/knowledge/index/{documentId}")
    @Operation(summary = "删除索引", description = "删除文档索引")
    public Result<Map<String, Object>> deleteIndex(
            @Parameter(description = "文档ID") @PathVariable String documentId) {
        Map<String, Object> metadata = knowledgeBase.getDocumentMetadata(documentId);
        if (!requestUserContext.isAdmin()
                && !requestUserContext.getRequiredUserId().equals(String.valueOf(metadata.getOrDefault("userId", "")))) {
            throw new SecurityException("无权删除该索引");
        }
        Map<String, Object> result = knowledgeIndexAgent.deleteIndex(documentId);
        return Result.success(result);
    }

    /**
     * 知识索引Agent - 异步索引文档
     */
    @PostMapping("/knowledge/index/async")
    @Operation(summary = "异步索引文档", description = "立即返回任务ID，索引在后台异步执行")
    public Result<Map<String, Object>> indexDocumentAsync(
            @Parameter(description = "文档ID") @RequestParam String documentId,
            @Parameter(description = "知识库ID") @RequestParam(defaultValue = "default") String knowledgeBaseId,
            @Parameter(description = "文档内容") @RequestBody String content) {
        Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("userId", requestUserContext.getRequiredUserId());
        metadata.put("knowledgeBaseId", knowledgeBaseId);
        return Result.success(knowledgeIndexAgent.indexDocumentAsync(documentId, content, metadata));
    }

    /**
     * 知识索引Agent - 查询任务状态
     */
    @GetMapping("/knowledge/jobs/{jobId}")
    @Operation(summary = "查询索引任务状态", description = "返回任务状态、质量评估、错误信息")
    public Result<Map<String, Object>> getKnowledgeJob(@PathVariable String jobId) {
        return Result.success(knowledgeIndexAgent.getJobStatus(jobId));
    }

    /**
     * 知识索引Agent - 重试失败任务
     */
    @PostMapping("/knowledge/jobs/{jobId}/retry")
    @Operation(summary = "重试失败索引任务", description = "对状态为 FAILED 的任务重新执行索引管道")
    public Result<Map<String, Object>> retryKnowledgeJob(
            @PathVariable String jobId,
            @RequestBody String content) {
        Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("userId", requestUserContext.getRequiredUserId());
        return Result.success(knowledgeIndexAgent.retryJob(jobId, content, metadata));
    }

    /**
     * 知识索引Agent - 列出当前用户的索引任务，包含进度、质量、复用状态。
     */
    @GetMapping("/knowledge/jobs")
    @Operation(summary = "列出索引任务", description = "按用户/知识库过滤，返回任务进度、质量评估、是否复用旧索引")
    public Result<List<Map<String, Object>>> listKnowledgeJobs(
            @Parameter(description = "知识库ID, 缺省返回全部") @RequestParam(required = false) String knowledgeBaseId,
            @Parameter(description = "管理员可指定 userId") @RequestParam(required = false) String userId) {
        String target;
        if (requestUserContext.isAdmin()) {
            target = userId; // 管理员未指定时为 null，返回全部
        } else {
            target = requestUserContext.getRequiredUserId();
        }
        return Result.success(knowledgeIndexAgent.listJobs(target, knowledgeBaseId));
    }

    /**
     * 知识索引Agent - 删除索引任务记录。
     */
    @DeleteMapping("/knowledge/jobs/{jobId}")
    @Operation(summary = "删除索引任务记录", description = "仅删除任务追踪数据，不影响实际向量索引；如需删除索引请使用 /knowledge/index/{documentId}")
    public Result<Boolean> deleteKnowledgeJob(@PathVariable String jobId) {
        Map<String, Object> snapshot = knowledgeIndexAgent.getJobStatus(jobId);
        if (snapshot != null && !"not_found".equals(snapshot.get("status"))
                && !requestUserContext.isAdmin()
                && !requestUserContext.getRequiredUserId().equals(String.valueOf(snapshot.getOrDefault("userId", "")))) {
            throw new SecurityException("无权删除该任务");
        }
        return Result.success(knowledgeIndexAgent.deleteJob(jobId));
    }

    /**
     * Agent 工作台 - 聚合接口，返回工具/对话/任务等综合数据。
     */
    @GetMapping("/workbench/overview")
    @Operation(summary = "Agent工作台总览", description = "聚合工具列表、用户对话、最近Agent任务和索引任务用于前端展示")
    public Result<Map<String, Object>> workbenchOverview() {
        String userId = requestUserContext.getRequiredUserId();
        Map<String, Object> overview = new java.util.LinkedHashMap<>();
        overview.put("tools", agentExecutionService.listTools());
        overview.put("conversations", conversationManager.getUserConversations(userId));
        overview.put("tasks", agentTaskRegistry.listByUser(userId));
        overview.put("knowledgeJobs", knowledgeIndexAgent.listJobs(userId, null));
        overview.put("user", Map.of(
                "userId", userId,
                "role", requestUserContext.getCurrentRole()
        ));
        return Result.success(overview);
    }

    /**
     * 规划执行Agent - 执行任务
     */
    @PostMapping("/plan/execute")
    @Operation(summary = "执行规划任务", description = "兼容旧接口，内部使用统一Agent链路执行复杂任务")
    public Result<Map<String, Object>> executePlan(
            @Parameter(description = "任务描述") @RequestBody String task) {
        AgentExecutionRequest request = new AgentExecutionRequest();
        request.setTask(task);
        request.setUserId(requestUserContext.getRequiredUserId());
        request.setContext(Map.of());
        Map<String, Object> result = agentExecutionService.execute(request);
        return Result.success(result);
    }
}
