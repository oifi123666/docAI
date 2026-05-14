package com.javaee.aiservice.agent.execution;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaee.aiservice.agent.ChatService;
import com.javaee.aiservice.agent.PromptEngineeringService;
import com.javaee.aiservice.agent.execution.approval.AgentApprovalService;
import com.javaee.aiservice.agent.execution.event.AgentProgressBroadcaster;
import com.javaee.aiservice.agent.execution.event.AgentProgressEvent;
import com.javaee.aiservice.agent.execution.model.AgentExecutionRequest;
import com.javaee.aiservice.agent.execution.model.AgentPlanStep;
import com.javaee.aiservice.agent.execution.model.AgentStepStatus;
import com.javaee.aiservice.agent.execution.model.AgentToolResult;
import com.javaee.aiservice.agent.execution.reflection.AgentReflection;
import com.javaee.aiservice.agent.execution.reflection.AgentReflectionService;
import com.javaee.aiservice.agent.execution.task.AgentTaskRegistry;
import com.javaee.aiservice.agent.execution.tool.AgentToolDefinition;
import com.javaee.aiservice.agent.execution.tool.AgentToolParameterDefinition;
import com.javaee.aiservice.agent.execution.tool.AgentToolRegistry;
import com.javaee.aiservice.conversation.ContextManager;
import com.javaee.aiservice.conversation.ConversationManager;
import com.javaee.aiservice.dto.FileDeleteDTO;
import com.javaee.aiservice.dto.FileDownloadDTO;
import com.javaee.aiservice.dto.FileRestoreDTO;
import com.javaee.aiservice.dto.FileVersionDTO;
import com.javaee.aiservice.dto.FileVersionSwitchDTO;
import com.javaee.aiservice.dto.KeywordExtractDTO;
import com.javaee.aiservice.dto.TextAnalyzeDTO;
import com.javaee.aiservice.dto.TextSummarizeDTO;
import com.javaee.aiservice.internal.InternalService;
import com.javaee.aiservice.rag.KnowledgeBase;
import com.javaee.aiservice.rag.Reranker;
import com.javaee.aiservice.security.RequestUserContext;
import com.javaee.aiservice.service.AIService;
import com.javaee.aiservice.service.FileDeleteService;
import com.javaee.aiservice.service.FileDownloadService;
import com.javaee.aiservice.service.FileVersionService;
import com.javaee.aiservice.service.RecycleBinService;
import com.javaee.aiservice.skills.SkillExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Product-level Agent chain: plan -> execute tools -> synthesize answer -> persist conversation.
 */
@Service
public class AgentExecutionService {

    private static final Logger log = LoggerFactory.getLogger(AgentExecutionService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ChatService chatService;

    @Autowired
    private PromptEngineeringService promptEngineeringService;

    @Autowired
    private AIService aiService;

    @Autowired
    private KnowledgeBase knowledgeBase;

    @Autowired
    private ConversationManager conversationManager;

    @Autowired
    private ContextManager contextManager;

    @Autowired
    private InternalService internalService;

    @Autowired
    private FileDownloadService fileDownloadService;

    @Autowired
    private FileDeleteService fileDeleteService;

    @Autowired
    private FileVersionService fileVersionService;

    @Autowired
    private RecycleBinService recycleBinService;

    @Autowired
    private SkillExecutorService skillExecutorService;

    @Autowired
    private AgentToolRegistry toolRegistry;

    @Autowired
    private AgentApprovalService agentApprovalService;

    @Autowired
    private RequestUserContext requestUserContext;

    @Autowired
    private AgentTaskRegistry taskRegistry;

    @Autowired
    private AgentProgressBroadcaster progressBroadcaster;

    @Autowired
    private AgentReflectionService reflectionService;

    @Value("${ai.agent.reflection.enabled:true}")
    private boolean defaultReflectionEnabled;

    public Map<String, Object> execute(AgentExecutionRequest request) {
        validateRequest(request);

        long startedAt = System.currentTimeMillis();
        String traceId = UUID.randomUUID().toString();
        String userId = requestUserContext.getRequiredUserId();
        request.setUserId(userId);
        String conversationId = ensureConversation(request.getConversationId(), userId);
        Map<String, Object> context = mergeContext(conversationId, request.getContext());
        context.put("userId", userId);
        context.put("role", requestUserContext.getCurrentRole());
        context.put("traceId", traceId);
        context.put("knowledgeBaseId", valueOrDefault(request.getKnowledgeBaseId(), "default"));

        log.info("开始执行Agent链路: traceId={}, conversationId={}, task={}", traceId, conversationId, request.getTask());
        publishTaskEvent("task_started", traceId, userId, "running", 0, "Agent 任务开始", Map.of("task", request.getTask()));

        int maxIterations = Math.max(1, Math.min(intValue(request.getMaxIterations(), 3), 5));
        int maxToolCalls = Math.max(1, Math.min(intValue(request.getMaxToolCalls(), 8), 20));
        List<AgentPlanStep> plan = new ArrayList<>();
        List<AgentToolResult> toolResults = new ArrayList<>();
        List<AgentReflection> reflections = new ArrayList<>();
        List<Map<String, Object>> timeline = new ArrayList<>();
        Set<String> executedSignatures = new LinkedHashSet<>();
        List<AgentPlanStep> reflectionPlan = List.of();
        boolean requiresAction = false;
        String stoppedReason = "completed";
        int toolCallCount = 0;
        int iterations = 0;

        for (int iteration = 1; iteration <= maxIterations; iteration++) {
            iterations = iteration;
            if (taskRegistry.isCancelled(traceId)) {
                stoppedReason = "cancelled";
                publishTaskEvent("task_cancelled", traceId, userId, "cancelled", 100, "Agent 任务已取消", Map.of());
                break;
            }
            List<AgentPlanStep> iterationPlan = buildIterationPlan(request, context, plan, toolResults, iteration, reflectionPlan);
            reflectionPlan = List.of();
            if (iterationPlan.isEmpty()) {
                stoppedReason = "no_plan";
                break;
            }

            for (AgentPlanStep step : iterationPlan) {
                step.setId("iter-" + iteration + "-" + valueOrDefault(step.getId(), "step-" + (plan.size() + 1)));
                plan.add(step);

                if (Boolean.TRUE.equals(request.getDryRun())) {
                    step.setStatus(AgentStepStatus.PLANNED.value());
                    publishTaskEvent("step_planned", traceId, userId, step.getStatus(), progressOf(plan.size(), maxToolCalls),
                            "步骤已规划: " + step.getToolName(), timelineEvent(iteration, step, null));
                    continue;
                }

                if (toolCallCount >= maxToolCalls) {
                    AgentToolResult result = AgentToolResult.error(step.getToolName(), "已达到工具调用上限: " + maxToolCalls);
                    decorateResult(result, step, System.currentTimeMillis());
                    step.setStatus(result.getStatus());
                    step.setObservation(result.getMessage());
                    toolResults.add(result);
                    stoppedReason = "tool_call_limit";
                    break;
                }

                if (!areDependenciesSatisfied(step, plan)) {
                    AgentToolResult result = AgentToolResult.error(step.getToolName(),
                            "依赖步骤未成功完成: " + step.getDependsOn());
                    decorateResult(result, step, System.currentTimeMillis());
                    step.setStatus(AgentStepStatus.BLOCKED.value());
                    step.setObservation(result.getMessage());
                    toolResults.add(result);
                    timeline.add(timelineEvent(iteration, step, result));
                    stoppedReason = "dependency_failed";
                    break;
                }

                String signature = step.getToolName() + ":" + safeJson(step.getParams());
                if (executedSignatures.contains(signature) && !"direct-answer".equals(step.getToolName())) {
                    step.setStatus(AgentStepStatus.SKIPPED.value());
                    step.setObservation("跳过重复工具调用");
                    timeline.add(timelineEvent(iteration, step, null));
                    continue;
                }
                executedSignatures.add(signature);

                AgentToolResult result = runStepWithRetry(step, request, context, iteration, timeline);
                publishTaskEvent("step_finished", traceId, userId, step.getStatus(), progressOf(toolCallCount + 1, maxToolCalls),
                        result.getMessage(), timelineEvent(iteration, step, result));
                toolResults.add(result);
                toolCallCount++;
                mergeToolResultIntoContext(context, result, step);
                internalService.logAudit(step.getToolName(), step.getParams(), objectMapper.convertValue(result, new TypeReference<>() {}));

                if ("error".equals(result.getStatus()) || result.isRequiresAction()) {
                    requiresAction = result.isRequiresAction();
                    stoppedReason = result.isRequiresAction() ? "action_required" : "tool_error";
                    if ("error".equals(result.getStatus())) {
                        break;
                    }
                }
            }

            if (Boolean.TRUE.equals(request.getDryRun())) {
                stoppedReason = "dry_run";
                break;
            }

            AgentReflection reflection = reflectAfterIteration(request, plan, toolResults, context,
                    iteration, maxIterations, traceId, userId);
            if (reflection != null) {
                reflections.add(reflection);
                context.put("lastReflection", reflection);
                timeline.add(reflectionTimelineEvent(iteration, reflection));
            }

            if (requiresAction || "tool_call_limit".equals(stoppedReason)) {
                break;
            }
            if (iteration >= maxIterations) {
                if (reflection != null && !reflection.isComplete()) {
                    stoppedReason = "max_iterations";
                }
                break;
            }
            if (!Boolean.TRUE.equals(request.getAutoReplan())) {
                if (!"tool_error".equals(stoppedReason) && !"dependency_failed".equals(stoppedReason)) {
                    stoppedReason = "completed";
                }
                break;
            }

            if (isReflectionEnabled(request)) {
                if (reflection == null || reflection.isComplete() || !reflection.isContinueExecution()) {
                    if (!"tool_error".equals(stoppedReason) && !"dependency_failed".equals(stoppedReason)) {
                        stoppedReason = "completed";
                    }
                    break;
                }
                reflectionPlan = normalizePlan(reflection.getRevisedPlan(), request, context);
                if (reflectionPlan.isEmpty()) {
                    stoppedReason = "reflection_no_plan";
                    break;
                }
                stoppedReason = "reflection_replan";
                continue;
            }

            if ("tool_error".equals(stoppedReason) || "dependency_failed".equals(stoppedReason)) {
                break;
            }
            if (!shouldContinue(request, plan, toolResults, context, iteration, maxIterations)) {
                stoppedReason = "completed";
                break;
            }
        }

        String finalAnswer = synthesizeAnswer(request, plan, toolResults, context, requiresAction);
        conversationManager.addMessageForUser(conversationId, userId, request.getTask(), finalAnswer);
        context.put("lastAnswer", finalAnswer);
        context.put("lastToolResults", toolResults);
        context.put("lastReflections", reflections);
        contextManager.updateContext(conversationId, context);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", requiresAction ? "action_required" : resolveStatus(toolResults));
        response.put("traceId", traceId);
        response.put("conversationId", conversationId);
        response.put("task", request.getTask());
        response.put("plan", plan);
        response.put("toolResults", toolResults);
        response.put("reflections", Boolean.FALSE.equals(request.getReturnIntermediateSteps()) ? List.of() : reflections);
        response.put("timeline", Boolean.FALSE.equals(request.getReturnIntermediateSteps()) ? List.of() : timeline);
        response.put("answer", finalAnswer);
        response.put("context", contextManager.getContext(conversationId));
        response.put("availableTools", toolRegistry.list());
        response.put("iterations", iterations);
        response.put("toolCallCount", toolCallCount);
        response.put("stoppedReason", stoppedReason);
        response.put("durationMs", System.currentTimeMillis() - startedAt);
        response.put("userId", userId);
        taskRegistry.save(traceId, response);
        publishTaskEvent("task_finished", traceId, userId, String.valueOf(response.get("status")), 100,
                "Agent 任务结束: " + stoppedReason, response);
        return response;
    }

    public List<AgentToolDefinition> listTools() {
        return toolRegistry.list();
    }

    private void publishTaskEvent(String eventType, String traceId, String userId, String status,
                                  int progress, String message, Map<String, Object> payload) {
        if (progressBroadcaster == null) {
            return;
        }
        AgentProgressEvent event = AgentProgressEvent.of(eventType, userId, status, message);
        event.setTraceId(traceId);
        event.setProgress(progress);
        event.setPayload(payload);
        progressBroadcaster.publish(event);
    }

    private int progressOf(int current, int total) {
        int safeTotal = Math.max(1, total);
        return Math.max(0, Math.min(95, (int) Math.round((current * 100.0) / safeTotal)));
    }

    private Map<String, Object> reflectionTimelineEvent(int iteration, AgentReflection reflection) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "reflection");
        event.put("iteration", iteration);
        event.put("complete", reflection.isComplete());
        event.put("continueExecution", reflection.isContinueExecution());
        event.put("requiresReplan", reflection.isRequiresReplan());
        event.put("qualityScore", reflection.getQualityScore());
        event.put("confidence", reflection.getConfidence());
        event.put("reason", reflection.getReason());
        event.put("issues", reflection.getIssues());
        event.put("missingInfo", reflection.getMissingInfo());
        event.put("recommendedActions", reflection.getRecommendedActions());
        event.put("revisedPlan", reflection.getRevisedPlan());
        event.put("source", reflection.getSource());
        event.put("fallback", reflection.isFallback());
        event.put("createdAt", reflection.getCreatedAt());
        return event;
    }

    private boolean isReflectionEnabled(AgentExecutionRequest request) {
        return request.getReflectionEnabled() == null
                ? defaultReflectionEnabled
                : Boolean.TRUE.equals(request.getReflectionEnabled());
    }

    /**
     * 对历史任务的单个步骤进行重试，返回最新工具结果与原任务快照。
     * 仅用于已结束（success/error/cancelled）任务的针对性补救：不会触发整轮 replan，
     * 也不会修改原始 timeline，结果通过 `singleStepRetry` 字段附加到 snapshot。
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> retryStep(String traceId, String stepId, Map<String, Object> overrideParams) {
        Map<String, Object> snapshot = taskRegistry.get(traceId);
        if (snapshot == null || snapshot.isEmpty()) {
            return Map.of("status", "not_found", "traceId", traceId);
        }
        Object planObj = snapshot.get("plan");
        if (!(planObj instanceof List<?> planList)) {
            return Map.of("status", "no_plan", "traceId", traceId);
        }
        AgentPlanStep target = null;
        for (Object item : planList) {
            if (item instanceof AgentPlanStep s && stepId.equals(s.getId())) {
                target = s;
                break;
            }
        }
        if (target == null) {
            return Map.of("status", "step_not_found", "traceId", traceId, "stepId", stepId);
        }
        if (overrideParams != null && !overrideParams.isEmpty()) {
            target.getParams().putAll(overrideParams);
        }
        AgentExecutionRequest request = new AgentExecutionRequest();
        request.setTask(asString(snapshot.get("task")));
        request.setUserId(asString(snapshot.get("userId")));
        request.setConversationId(asString(snapshot.get("conversationId")));
        Map<String, Object> ctx = snapshot.get("context") instanceof Map<?, ?> raw
                ? new HashMap<>((Map<String, Object>) raw)
                : new HashMap<>();
        long started = System.currentTimeMillis();
        target.setStatus(AgentStepStatus.RUNNING.value());
        AgentToolResult result = executeStep(target, request, ctx);
        decorateResult(result, target, started);
        target.setStatus(normalizeStatus(result, target));
        target.setObservation(result.getMessage());
        target.setAttempts(target.getAttempts() + 1);

        Map<String, Object> retryInfo = new LinkedHashMap<>();
        retryInfo.put("stepId", stepId);
        retryInfo.put("status", result.getStatus());
        retryInfo.put("observation", result.getMessage());
        retryInfo.put("data", result.getData());
        retryInfo.put("attempts", target.getAttempts());
        retryInfo.put("retriedAt", System.currentTimeMillis());
        snapshot.put("singleStepRetry", retryInfo);
        taskRegistry.save(traceId, snapshot);

        Map<String, Object> response = new LinkedHashMap<>(retryInfo);
        response.put("traceId", traceId);
        return response;
    }

    private List<AgentPlanStep> buildIterationPlan(AgentExecutionRequest request, Map<String, Object> context,
                                                   List<AgentPlanStep> previousPlan,
                                                   List<AgentToolResult> previousResults,
                                                   int iteration,
                                                   List<AgentPlanStep> reflectedPlan) {
        if (reflectedPlan != null && !reflectedPlan.isEmpty()) {
            return reflectedPlan;
        }
        if (iteration == 1) {
            return buildPlan(request, context);
        }
        try {
            String raw = chatService.callChatApiWithModelCode(
                    buildFollowUpPlannerPrompt(request, context, previousPlan, previousResults, iteration),
                    request.getModel());
            List<AgentPlanStep> plan = parsePlan(raw);
            if (!plan.isEmpty()) {
                return normalizePlan(plan, request, context);
            }
        } catch (Exception e) {
            log.warn("模型补充规划失败，结束自动重规划: {}", e.getMessage());
        }
        return List.of();
    }

    private AgentReflection reflectAfterIteration(AgentExecutionRequest request, List<AgentPlanStep> plan,
                                                 List<AgentToolResult> results, Map<String, Object> context,
                                                 int iteration, int maxIterations, String traceId, String userId) {
        if (!isReflectionEnabled(request) || results.isEmpty()) {
            return null;
        }
        AgentReflection reflection = reflectionService.reflect(request, plan, results, context, iteration, maxIterations);
        publishTaskEvent("reflection_finished", traceId, userId,
                reflection.isComplete() ? "complete" : "needs_more_work",
                progressOf(iteration, maxIterations),
                valueOrDefault(reflection.getReason(), "Agent 反思完成"),
                objectMapper.convertValue(reflection, new TypeReference<>() {}));
        return reflection;
    }

    private String buildFollowUpPlannerPrompt(AgentExecutionRequest request, Map<String, Object> context,
                                              List<AgentPlanStep> previousPlan,
                                              List<AgentToolResult> previousResults,
                                              int iteration) {
        return """
                你是DocAI的Agent执行器，正在进行第%s轮补充规划。请只输出JSON数组，不要输出解释、Markdown或代码块。
                如果任务已经可以回答，请输出空数组 []。
                如果还需要工具，请输出最多3个后续步骤，每个元素包含: id, description, toolName, params, reasoning。
                只能使用工具列表中的toolName；不要重复已经成功执行且参数相同的工具。

                工具列表:
                %s

                用户任务:
                %s

                已执行计划:
                %s

                工具观察结果:
                %s

                当前上下文:
                %s
                """.formatted(iteration, toolCatalog(), request.getTask(), safeJson(previousPlan),
                safeJson(previousResults), safeJson(context));
    }

    private boolean shouldContinue(AgentExecutionRequest request, List<AgentPlanStep> plan,
                                   List<AgentToolResult> results, Map<String, Object> context,
                                   int iteration, int maxIterations) {
        if (iteration >= maxIterations || results.isEmpty()) {
            return false;
        }
        AgentToolResult last = results.get(results.size() - 1);
        if ("error".equals(last.getStatus()) || last.isRequiresAction()) {
            return false;
        }
        if ("direct-answer".equals(last.getToolName())) {
            return false;
        }
        if (hasFinalAnswerSignal(last)) {
            return false;
        }

        try {
            String prompt = """
                    你是DocAI Agent的完成度评估器。请只输出JSON对象，不要输出解释。
                    格式: {"continue":true/false,"reason":"简短原因"}

                    判断规则:
                    - 如果工具结果已经足够回答用户任务，continue=false。
                    - 如果还缺少必要信息、需要追加检索、需要继续调用工具，continue=true。
                    - 不要为了无意义优化继续循环。

                    用户任务:
                    %s

                    当前计划:
                    %s

                    工具结果:
                    %s

                    上下文:
                    %s
                    """.formatted(request.getTask(), safeJson(plan), safeJson(results), safeJson(context));
            String raw = chatService.callChatApiWithModelCode(prompt, request.getModel());
            Map<String, Object> decision = objectMapper.readValue(stripObjectJson(raw), new TypeReference<>() {});
            return booleanValue(decision.get("continue"), false);
        } catch (Exception e) {
            log.debug("完成度评估失败，默认结束当前Agent循环: {}", e.getMessage());
            return false;
        }
    }

    private boolean hasFinalAnswerSignal(AgentToolResult result) {
        return result.getData().containsKey("answer")
                || result.getData().containsKey("fileUrl")
                || result.getData().containsKey("result");
    }

    private void decorateResult(AgentToolResult result, AgentPlanStep step, long startedAt) {
        long finishedAt = System.currentTimeMillis();
        result.setStepId(step.getId());
        result.setStartedAt(startedAt);
        result.setFinishedAt(finishedAt);
        result.setDurationMs(Math.max(0, finishedAt - startedAt));
    }

    private Map<String, Object> timelineEvent(int iteration, AgentPlanStep step, AgentToolResult result) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("iteration", iteration);
        event.put("stepId", step.getId());
        event.put("description", step.getDescription());
        event.put("toolName", step.getToolName());
        event.put("status", step.getStatus());
        event.put("observation", step.getObservation());
        if (result != null) {
            event.put("durationMs", result.getDurationMs());
            event.put("resultSummary", result.getMessage());
        }
        return event;
    }

    @SuppressWarnings("unchecked")
    private void mergeToolResultIntoContext(Map<String, Object> context, AgentToolResult result, AgentPlanStep step) {
        context.put("lastTool", result.getToolName());
        context.put("lastToolStatus", result.getStatus());
        context.put("lastObservation", result.getMessage());
        if ("success".equals(result.getStatus())) {
            for (Map.Entry<String, Object> entry : result.getData().entrySet()) {
                if (entry.getValue() != null && isSafeContextKey(entry.getKey())) {
                    context.put(entry.getKey(), entry.getValue());
                }
            }
        }
        if (step != null && step.getId() != null) {
            Object existing = context.get("__stepResults__");
            Map<String, Object> stepResults = existing instanceof Map
                    ? (Map<String, Object>) existing
                    : new LinkedHashMap<>();
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("status", result.getStatus());
            snapshot.put("observation", result.getMessage());
            snapshot.put("data", result.getData());
            stepResults.put(step.getId(), snapshot);
            // 同时按短 id（去掉 iter- 前缀）建立别名，方便模板引用
            String shortId = step.getId().contains("-")
                    ? step.getId().substring(step.getId().lastIndexOf('-') + 1)
                    : step.getId();
            stepResults.putIfAbsent(shortId, snapshot);
            context.put("__stepResults__", stepResults);
        }
    }

    private boolean isSafeContextKey(String key) {
        return Set.of("answer", "sources", "retrieved", "results", "fileUrl", "objectName",
                "bucketName", "recycleId", "versionId", "documentId", "query").contains(key);
    }

    private void validateRequest(AgentExecutionRequest request) {
        if (request == null || isBlank(request.getTask())) {
            throw new IllegalArgumentException("任务描述不能为空");
        }
    }

    private String ensureConversation(String conversationId, String userId) {
        if (!isBlank(conversationId)) {
            conversationManager.assertOwner(conversationId, userId);
            return conversationId;
        }
        return conversationManager.createConversation(userId);
    }

    private Map<String, Object> mergeContext(String conversationId, Map<String, Object> requestContext) {
        Map<String, Object> merged = new HashMap<>(contextManager.getContext(conversationId));
        if (requestContext != null) {
            merged.putAll(requestContext);
        }
        return merged;
    }

    private List<AgentPlanStep> buildPlan(AgentExecutionRequest request, Map<String, Object> context) {
        try {
            String prompt = buildPlannerPrompt(request, context);
            String raw = chatService.callChatApiWithModelCode(prompt, request.getModel());
            List<AgentPlanStep> plan = parsePlan(raw);
            if (!plan.isEmpty()) {
                return normalizePlan(plan, request, context);
            }
        } catch (Exception e) {
            log.warn("模型规划失败，使用规则兜底规划: {}", e.getMessage());
        }
        return fallbackPlan(request, context);
    }

    private String buildPlannerPrompt(AgentExecutionRequest request, Map<String, Object> context) {
        return """
                你是DocAI的任务规划器。请只输出JSON数组，不要输出解释、Markdown或代码块。
                每个数组元素包含: id, description, toolName, params, reasoning。
                只能从工具列表中选择toolName；如不需要工具，使用direct-answer。
                多步任务可以输出多个步骤，但不要超过5步；参数必须来自用户任务或上下文，不确定时先用direct-answer询问澄清。
                对危险操作(file-delete,file-version-switch)必须设置requireConfirmation=true。

                工具列表:
                %s

                用户任务:
                %s

                上下文:
                %s
                """.formatted(toolCatalog(), request.getTask(), safeJson(context));
    }

    private String toolCatalog() {
        StringBuilder tools = new StringBuilder();
        for (AgentToolDefinition tool : toolRegistry.list()) {
            tools.append("- ").append(tool.getName()).append(": ").append(tool.getDescription())
                    .append(" 类别: ").append(tool.getCategory())
                    .append(" 风险等级: ").append(tool.getRiskLevel())
                    .append(" 危险操作: ").append(tool.isDestructive())
                    .append(" 需要用户动作: ").append(tool.isRequiresUserAction())
                    .append(" 参数schema: ").append(safeJson(tool.getParameterSchema())).append("\n");
        }
        tools.append("提示: 如果用户任务缺少必填参数或存在歧义，请优先选择 ask-user 工具向用户澄清，不要凭空填充参数。\n");
        return tools.toString();
    }

    private boolean areDependenciesSatisfied(AgentPlanStep step, List<AgentPlanStep> plan) {
        if (step.getDependsOn() == null || step.getDependsOn().isEmpty()) {
            return true;
        }
        Map<String, AgentPlanStep> indexed = new HashMap<>();
        for (AgentPlanStep prior : plan) {
            indexed.put(prior.getId(), prior);
            if (prior.getId() != null && prior.getId().contains("-")) {
                indexed.putIfAbsent(prior.getId().substring(prior.getId().lastIndexOf('-') + 1), prior);
            }
        }
        for (String dependency : step.getDependsOn()) {
            AgentPlanStep prior = indexed.get(dependency);
            if (prior == null || !AgentStepStatus.isSuccess(prior.getStatus())) {
                return false;
            }
        }
        return true;
    }

    private AgentToolResult runStepWithRetry(AgentPlanStep step, AgentExecutionRequest request,
                                             Map<String, Object> context, int iteration,
                                             List<Map<String, Object>> timeline) {
        int maxAttempts = Math.max(1, step.getMaxRetries() + 1);
        AgentToolResult lastResult = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            step.setAttempts(attempt);
            long stepStartedAt = System.currentTimeMillis();
            step.setStatus(AgentStepStatus.RUNNING.value());
            AgentToolResult result = executeStep(step, request, context);
            decorateResult(result, step, stepStartedAt);
            step.setStatus(normalizeStatus(result, step));
            step.setObservation(result.getMessage());

            if (AgentStepStatus.SUCCESS.value().equals(result.getStatus()) && !isBlank(step.getSuccessCriteria())) {
                String unmet = evaluateSuccessCriteria(step, result);
                if (unmet != null) {
                    AgentToolResult downgraded = AgentToolResult.error(step.getToolName(), "未满足 successCriteria: " + unmet);
                    downgraded.setData(result.getData());
                    decorateResult(downgraded, step, stepStartedAt);
                    step.setStatus(AgentStepStatus.ERROR.value());
                    step.setObservation(downgraded.getMessage());
                    step.setThought("结果未达到 successCriteria，准备重试或 replan: " + unmet);
                    timeline.add(timelineEvent(iteration, step, downgraded));
                    lastResult = downgraded;
                    if ("none".equalsIgnoreCase(step.getRetryPolicy()) || attempt >= maxAttempts) {
                        step.setLastError(downgraded.getMessage());
                        return downgraded;
                    }
                    step.setLastError(downgraded.getMessage());
                    sleepBackoff(step.getRetryPolicy(), attempt);
                    continue;
                }
            }

            step.setThought(buildThought(step, result, attempt, maxAttempts));
            timeline.add(timelineEvent(iteration, step, result));
            lastResult = result;

            if (!"error".equals(result.getStatus()) || result.isRequiresAction()) {
                return result;
            }
            if ("none".equalsIgnoreCase(step.getRetryPolicy()) || attempt >= maxAttempts) {
                step.setLastError(result.getMessage());
                return result;
            }
            step.setLastError(result.getMessage());
            sleepBackoff(step.getRetryPolicy(), attempt);
        }
        return lastResult;
    }

    /**
     * 评估 successCriteria：支持以分号分隔的若干断言：
     * - contains:keyword       observation 中必须包含 keyword
     * - data.<key>             data 中必须存在该字段且非空
     * - data.<key>=value       data.key 必须等于 value
     * 返回 null 表示通过，否则返回未满足的条目。
     */
    String evaluateSuccessCriteria(AgentPlanStep step, AgentToolResult result) {
        String criteria = step.getSuccessCriteria();
        if (isBlank(criteria)) {
            return null;
        }
        String observation = valueOrDefault(result.getMessage(), "");
        Map<String, Object> data = result.getData() == null ? Map.of() : result.getData();
        for (String raw : criteria.split("[;；]")) {
            String token = raw.trim();
            if (token.isEmpty()) {
                continue;
            }
            if (token.startsWith("contains:")) {
                String keyword = token.substring("contains:".length()).trim();
                if (!observation.contains(keyword) && !safeJson(data).contains(keyword)) {
                    return token;
                }
            } else if (token.startsWith("data.")) {
                String expr = token.substring("data.".length());
                String key;
                String expected = null;
                int eq = expr.indexOf('=');
                if (eq > 0) {
                    key = expr.substring(0, eq).trim();
                    expected = expr.substring(eq + 1).trim();
                } else {
                    key = expr.trim();
                }
                Object actual = data.get(key);
                if (actual == null || actual.toString().isBlank()) {
                    return token;
                }
                if (expected != null && !expected.equals(actual.toString())) {
                    return token;
                }
            }
        }
        return null;
    }

    private String normalizeStatus(AgentToolResult result, AgentPlanStep step) {
        if (result.isRequiresAction()) {
            return "ask-user".equals(step.getToolName())
                    ? AgentStepStatus.WAITING_USER.value()
                    : AgentStepStatus.WAITING_USER.value();
        }
        return result.getStatus();
    }

    private String buildThought(AgentPlanStep step, AgentToolResult result, int attempt, int maxAttempts) {
        if (result.isRequiresAction()) {
            return "等待用户补充信息或确认: " + result.getMessage();
        }
        if (AgentStepStatus.SUCCESS.value().equals(result.getStatus())) {
            return "步骤 " + step.getId() + " 成功，可以继续后续依赖步骤。";
        }
        if (AgentStepStatus.ERROR.value().equals(result.getStatus())) {
            if (attempt < maxAttempts && !"none".equalsIgnoreCase(step.getRetryPolicy())) {
                return "步骤失败，将按 " + step.getRetryPolicy() + " 策略进行第 " + (attempt + 1) + " 次重试。";
            }
            return "步骤失败且已无重试余量，建议触发 replan 或返回错误。";
        }
        return "步骤进行中，等待结果。";
    }

    private void sleepBackoff(String policy, int attempt) {
        try {
            long base = "exponential".equalsIgnoreCase(policy) ? 200L * (1L << Math.min(attempt - 1, 4)) : 200L;
            Thread.sleep(Math.min(base, 2000L));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private List<AgentPlanStep> parsePlan(String raw) throws Exception {
        if (isBlank(raw)) {
            return List.of();
        }
        String json = stripJson(raw);
        List<Map<String, Object>> items = objectMapper.readValue(json, new TypeReference<>() {});
        List<AgentPlanStep> steps = new ArrayList<>();
        int index = 1;
        for (Map<String, Object> item : items) {
            AgentPlanStep step = new AgentPlanStep();
            step.setId(asString(item.getOrDefault("id", "step-" + index)));
            step.setDescription(asString(item.getOrDefault("description", item.getOrDefault("step", ""))));
            step.setToolName(asString(item.getOrDefault("toolName", item.getOrDefault("tool", "direct-answer"))));
            step.setReasoning(asString(item.get("reasoning")));
            step.setThought(asString(item.get("thought")));
            step.setSuccessCriteria(asString(item.get("successCriteria")));
            String retryPolicy = asString(item.get("retryPolicy"));
            if (!isBlank(retryPolicy)) {
                step.setRetryPolicy(retryPolicy);
            }
            Object maxRetries = item.get("maxRetries");
            if (maxRetries instanceof Number n) {
                step.setMaxRetries(n.intValue());
            }
            Object dependsOn = item.get("dependsOn");
            if (dependsOn instanceof List<?> list) {
                List<String> ids = new ArrayList<>();
                for (Object element : list) {
                    if (element != null) {
                        ids.add(element.toString());
                    }
                }
                step.setDependsOn(ids);
            }
            String riskLevel = asString(item.get("riskLevel"));
            if (!isBlank(riskLevel)) {
                step.setRiskLevel(riskLevel);
            }
            Object params = item.get("params");
            if (params instanceof Map<?, ?> map) {
                step.setParams(toStringObjectMap(map));
            }
            steps.add(step);
            index++;
        }
        return steps;
    }

    private List<AgentPlanStep> normalizePlan(List<AgentPlanStep> plan, AgentExecutionRequest request, Map<String, Object> context) {
        List<AgentPlanStep> normalized = new ArrayList<>();
        int index = 1;
        for (AgentPlanStep step : plan) {
            if (!toolRegistry.contains(step.getToolName())) {
                step.setToolName("direct-answer");
            }
            if (isBlank(step.getId())) {
                step.setId("step-" + index);
            }
            if (isBlank(step.getDescription())) {
                step.setDescription(request.getTask());
            }
            AgentToolDefinition definition = toolRegistry.get(step.getToolName());
            if (definition != null) {
                if (isBlank(step.getRiskLevel()) || "low".equals(step.getRiskLevel())) {
                    step.setRiskLevel(definition.getRiskLevel());
                }
                step.setRequiresApproval(definition.isDestructive());
            }
            if (isBlank(step.getRetryPolicy())) {
                step.setRetryPolicy(definition != null && definition.isDestructive() ? "none" : "exponential");
            }
            if (step.getMaxRetries() <= 0) {
                step.setMaxRetries(definition != null && definition.isDestructive() ? 0 : 1);
            }
            fillDefaultParams(step, request, context);
            normalized.add(step);
            index++;
        }
        return normalized;
    }

    private List<AgentPlanStep> fallbackPlan(AgentExecutionRequest request, Map<String, Object> context) {
        String task = request.getTask();
        String lower = task.toLowerCase(Locale.ROOT);
        String tool = "direct-answer";

        if (containsAny(task, "知识库", "文档库", "问答", "查询", "检索", "根据文档")) {
            tool = Boolean.FALSE.equals(request.getRagEnabled()) ? "direct-answer" : "rag-answer";
        } else if (containsAny(task, "总结", "摘要")) {
            tool = "text-summarize";
        } else if (containsAny(task, "关键词", "关键字")) {
            tool = "keyword-extract";
        } else if (containsAny(task, "统计", "分析")) {
            tool = "text-analyze";
        } else if (containsAny(task, "纠错", "润色", "改写", "优化")) {
            tool = "text-correct";
        } else if (lower.contains("ppt") || containsAny(task, "演示文稿", "幻灯片")) {
            tool = "html-ppt-generate";
        } else if (containsAny(task, "上传")) {
            tool = "file-upload";
        } else if (containsAny(task, "下载")) {
            tool = "file-download-url";
        } else if (containsAny(task, "删除")) {
            tool = "file-delete";
        } else if (containsAny(task, "恢复")) {
            tool = "file-restore";
        } else if (containsAny(task, "回收站")) {
            tool = "recycle-list";
        } else if (containsAny(task, "版本")) {
            tool = containsAny(task, "切换", "恢复到") ? "file-version-switch" : "file-version-list";
        }

        AgentPlanStep step = new AgentPlanStep("step-1", task, tool, new HashMap<>());
        fillDefaultParams(step, request, context);
        return List.of(step);
    }

    private void fillDefaultParams(AgentPlanStep step, AgentExecutionRequest request, Map<String, Object> context) {
        Map<String, Object> params = step.getParams();
        String content = firstNonBlank(asString(params.get("content")), asString(context.get("content")), request.getTask());
        String objectName = firstNonBlank(asString(params.get("objectName")), asString(context.get("objectName")), extractQuotedText(request.getTask()));

        switch (step.getToolName()) {
            case "rag-answer" -> {
                params.putIfAbsent("question", request.getTask());
                params.putIfAbsent("topK", 3);
                params.putIfAbsent("strategy", "HYBRID");
                params.putIfAbsent("userId", context.get("userId"));
                params.putIfAbsent("knowledgeBaseId", context.getOrDefault("knowledgeBaseId", "default"));
            }
            case "rag-search" -> {
                params.putIfAbsent("query", request.getTask());
                params.putIfAbsent("topK", 5);
                params.putIfAbsent("strategy", "HYBRID");
                params.putIfAbsent("userId", context.get("userId"));
                params.putIfAbsent("knowledgeBaseId", context.getOrDefault("knowledgeBaseId", "default"));
            }
            case "text-summarize" -> {
                params.putIfAbsent("content", content);
                params.putIfAbsent("maxLength", 300);
                params.putIfAbsent("model", request.getModel());
            }
            case "text-analyze", "keyword-extract", "text-correct" -> {
                params.putIfAbsent("content", content);
                params.putIfAbsent("count", 8);
                params.putIfAbsent("instruction", request.getTask());
                params.putIfAbsent("model", request.getModel());
            }
            case "file-download-url", "file-delete", "file-version-list", "file-version-switch" -> {
                params.putIfAbsent("objectName", objectName);
                params.putIfAbsent("bucketName", context.get("bucketName"));
                params.putIfAbsent("requireConfirmation", true);
            }
            case "file-restore" -> {
                params.putIfAbsent("recycleId", context.get("recycleId"));
                params.putIfAbsent("bucketName", context.get("bucketName"));
            }
            case "html-ppt-generate" -> {
                params.putIfAbsent("outline", content);
                params.putIfAbsent("title", firstNonBlank(asString(context.get("title")), "演示文稿"));
                params.putIfAbsent("theme", firstNonBlank(asString(context.get("theme")), "tokyo-night"));
                params.putIfAbsent("model", request.getModel());
            }
            default -> params.putIfAbsent("question", request.getTask());
        }
    }

    private AgentToolResult executeStep(AgentPlanStep step, AgentExecutionRequest request, Map<String, Object> context) {
        try {
            resolvePlaceholders(step, request, context);
            AgentToolResult validationResult = validateToolParameters(step);
            if (validationResult != null) {
                return validationResult;
            }
            AgentToolResult approvalResult = enforceDestructiveApproval(step, context);
            if (approvalResult != null) {
                return approvalResult;
            }
            if (!internalService.hasPermission(step.getToolName(), context)) {
                return AgentToolResult.error(step.getToolName(), "没有执行此工具的权限或缺少服务端确认");
            }
            return dispatchTool(step, request);
        } catch (Exception e) {
            log.error("工具执行失败: tool={}, step={}", step.getToolName(), step.getDescription(), e);
            return AgentToolResult.error(step.getToolName(), e.getMessage());
        }
    }

    /**
     * 在 ReAct loop 中支持模板变量引用：
     * ${task}                直接引用用户原始任务
     * ${context.userId}      引用 context 中的字段
     * ${steps.<id>.<field>}  引用前序步骤结果（observation/answer/...）
     */
    void resolvePlaceholders(AgentPlanStep step, AgentExecutionRequest request, Map<String, Object> context) {
        Map<String, Object> params = step.getParams();
        if (params == null || params.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : new ArrayList<>(params.entrySet())) {
            Object value = entry.getValue();
            if (value instanceof String text && text.contains("${")) {
                String resolved = renderTemplate(text, request, context);
                params.put(entry.getKey(), resolved);
            }
        }
    }

    private String renderTemplate(String text, AgentExecutionRequest request, Map<String, Object> context) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\$\\{([^}]+)}").matcher(text);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            String expression = matcher.group(1).trim();
            Object resolved = resolveExpression(expression, request, context);
            matcher.appendReplacement(buffer, java.util.regex.Matcher.quoteReplacement(
                    resolved == null ? "" : resolved.toString()));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    @SuppressWarnings("unchecked")
    private Object resolveExpression(String expression, AgentExecutionRequest request, Map<String, Object> context) {
        if ("task".equals(expression)) {
            return request.getTask();
        }
        String[] parts = expression.split("\\.");
        if (parts.length < 2) {
            return context.get(expression);
        }
        String root = parts[0];
        if ("context".equals(root)) {
            return drillInto(context, parts, 1);
        }
        if ("steps".equals(root)) {
            Object steps = context.get("__stepResults__");
            if (!(steps instanceof Map)) {
                return null;
            }
            String stepId = parts[1];
            Object stepData = ((Map<String, Object>) steps).get(stepId);
            if (parts.length == 2) {
                return stepData;
            }
            if (stepData instanceof Map<?, ?> map) {
                return drillInto((Map<String, Object>) map, parts, 2);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Object drillInto(Map<String, Object> source, String[] parts, int from) {
        Object current = source;
        for (int i = from; i < parts.length; i++) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = ((Map<String, Object>) map).get(parts[i]);
        }
        return current;
    }

    private AgentToolResult validateToolParameters(AgentPlanStep step) {
        AgentToolDefinition definition = toolRegistry.get(step.getToolName());
        if (definition == null) {
            return null;
        }

        Map<String, Object> params = step.getParams();
        List<String> missing = new ArrayList<>();
        Map<String, String> invalid = new LinkedHashMap<>();
        for (AgentToolParameterDefinition parameter : definition.getParameterSchema().values()) {
            Object value = params.get(parameter.getName());
            if (parameter.isRequired() && isValueMissing(value)) {
                missing.add(parameter.getName());
                continue;
            }
            if (isValueMissing(value)) {
                continue;
            }
            if (!isTypeCompatible(value, parameter.getType())) {
                invalid.put(parameter.getName(), "期望类型: " + parameter.getType());
                continue;
            }
            if (parameter.getAllowedValues() != null && !parameter.getAllowedValues().isEmpty()
                    && !parameter.getAllowedValues().contains(value)
                    && !parameter.getAllowedValues().contains(value.toString())) {
                invalid.put(parameter.getName(), "仅允许: " + parameter.getAllowedValues());
                continue;
            }
            if ("integer".equals(parameter.getType())) {
                long numeric;
                try {
                    numeric = Long.parseLong(value.toString());
                } catch (NumberFormatException ignore) {
                    invalid.put(parameter.getName(), "期望类型: integer");
                    continue;
                }
                if (parameter.getMinValue() != null && numeric < parameter.getMinValue().longValue()) {
                    invalid.put(parameter.getName(), "最小值: " + parameter.getMinValue());
                    continue;
                }
                if (parameter.getMaxValue() != null && numeric > parameter.getMaxValue().longValue()) {
                    invalid.put(parameter.getName(), "最大值: " + parameter.getMaxValue());
                    continue;
                }
            }
            if (parameter.getPattern() != null && !value.toString().matches(parameter.getPattern())) {
                invalid.put(parameter.getName(), "格式不匹配: " + parameter.getPattern());
            }
        }

        if (missing.isEmpty() && invalid.isEmpty()) {
            return null;
        }

        StringBuilder question = new StringBuilder("工具 ").append(definition.getName()).append(" 缺少必要信息: ");
        if (!missing.isEmpty()) {
            question.append("缺失参数 ").append(missing);
        }
        if (!invalid.isEmpty()) {
            if (!missing.isEmpty()) {
                question.append("; ");
            }
            question.append("非法参数 ").append(invalid.keySet());
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("toolName", definition.getName());
        data.put("missingParameters", missing);
        data.put("invalidParameters", invalid);
        data.put("parameterSchema", definition.getParameterSchema());
        data.put("question", question.toString());
        data.put("nextStep", "请补齐缺失或类型不正确的参数后，将参数放入 context 或工具 params 中重试。");
        return AgentToolResult.actionRequired(definition.getName(), question.toString(), data);
    }

    private boolean isTypeCompatible(Object value, String type) {
        if (value == null) {
            return false;
        }
        return switch (valueOrDefault(type, "string")) {
            case "integer" -> value instanceof Number || value.toString().matches("^-?\\d+$");
            case "boolean" -> value instanceof Boolean
                    || "true".equalsIgnoreCase(value.toString())
                    || "false".equalsIgnoreCase(value.toString());
            default -> true;
        };
    }

    private boolean isValueMissing(Object value) {
        return value == null || value.toString().trim().isEmpty() || "null".equalsIgnoreCase(value.toString().trim());
    }

    private AgentToolResult dispatchTool(AgentPlanStep step, AgentExecutionRequest request) {
        Map<String, Object> params = step.getParams();
        String tool = step.getToolName();

        return switch (tool) {
            case "direct-answer" -> executeDirectAnswer(request, params);
            case "ask-user" -> executeAskUser(params);
            case "rag-answer" -> executeRagAnswer(params, request.getModel());
            case "rag-search" -> executeRagSearch(params);
            case "text-summarize" -> executeSummarize(params);
            case "text-analyze" -> executeAnalyze(params);
            case "keyword-extract" -> executeKeywords(params);
            case "text-correct" -> executeTextCorrect(params);
            case "file-upload" -> AgentToolResult.actionRequired(tool, "文件上传需要通过 /api/ai/upload 或 /api/skills/file/upload 传入 MultipartFile。",
                    Map.of("uploadEndpoints", List.of("/api/ai/upload", "/api/skills/file/upload")));
            case "file-download-url" -> executeFileDownloadUrl(params);
            case "file-delete" -> executeFileDelete(params, request.getUserId());
            case "file-restore" -> executeFileRestore(params);
            case "recycle-list" -> AgentToolResult.success(tool, "回收站查询完成",
                    toMap(recycleBinService.listRecycleBin(asString(params.get("bucketName")), request.getUserId())));
            case "file-version-list" -> executeFileVersionList(params);
            case "file-version-switch" -> executeFileVersionSwitch(params);
            case "html-ppt-generate" -> executeHtmlPpt(params);
            default -> AgentToolResult.error(tool, "未知工具: " + tool);
        };
    }

    private AgentToolResult executeDirectAnswer(AgentExecutionRequest request, Map<String, Object> params) {
        String question = firstNonBlank(asString(params.get("question")), request.getTask());
        String answer = chatService.callChatApiWithModelCode(question, request.getModel());
        return AgentToolResult.success("direct-answer", "直接回答完成", Map.of("answer", answer));
    }

    private AgentToolResult executeAskUser(Map<String, Object> params) {
        String question = firstNonBlank(asString(params.get("question")), "请补充更多信息以便继续。");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("question", question);
        String missing = asString(params.get("missingFields"));
        if (!isBlank(missing)) {
            data.put("missingFields", List.of(missing.split("[,，;；]")));
        }
        String options = asString(params.get("options"));
        if (!isBlank(options)) {
            data.put("options", List.of(options.split("[;；]")));
        }
        return AgentToolResult.actionRequired("ask-user", question, data);
    }

    private AgentToolResult executeRagAnswer(Map<String, Object> params, String model) {
        String question = firstNonBlank(asString(params.get("question")), asString(params.get("query")));
        List<Map<String, Object>> results = searchKnowledge(question, intValue(params.get("topK"), 3),
                asString(params.get("strategy")), asString(params.get("userId")), asString(params.get("knowledgeBaseId")));
        String context = buildKnowledgeContext(results);
        String prompt = promptEngineeringService.createRagAnswerPrompt(question, context);
        String answer = chatService.callChatApiWithModelCode(prompt, model);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("question", question);
        data.put("answer", answer);
        data.put("sources", results.stream().map(r -> r.get("id")).toList());
        data.put("retrieved", results);
        return AgentToolResult.success("rag-answer", "知识库问答完成", data);
    }

    private AgentToolResult executeRagSearch(Map<String, Object> params) {
        String query = firstNonBlank(asString(params.get("query")), asString(params.get("question")));
        List<Map<String, Object>> results = searchKnowledge(query, intValue(params.get("topK"), 5),
                asString(params.get("strategy")), asString(params.get("userId")), asString(params.get("knowledgeBaseId")));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("query", query);
        data.put("results", results);
        data.put("sources", results.stream().map(r -> r.get("id")).toList());
        return AgentToolResult.success("rag-search", "知识库检索完成", data);
    }

    private AgentToolResult executeSummarize(Map<String, Object> params) {
        TextSummarizeDTO dto = new TextSummarizeDTO();
        dto.setContent(firstNonBlank(asString(params.get("content")), ""));
        dto.setMaxLength(intValue(params.get("maxLength"), 300));
        return AgentToolResult.success("text-summarize", "摘要完成", toMap(aiService.summarize(dto, asString(params.get("model")))));
    }

    private AgentToolResult executeAnalyze(Map<String, Object> params) {
        TextAnalyzeDTO dto = new TextAnalyzeDTO();
        dto.setContent(firstNonBlank(asString(params.get("content")), ""));
        return AgentToolResult.success("text-analyze", "文本分析完成", toMap(aiService.analyze(dto)));
    }

    private AgentToolResult executeKeywords(Map<String, Object> params) {
        KeywordExtractDTO dto = new KeywordExtractDTO();
        dto.setContent(firstNonBlank(asString(params.get("content")), ""));
        dto.setCount(intValue(params.get("count"), 8));
        return AgentToolResult.success("keyword-extract", "关键词提取完成", toMap(aiService.extractKeywords(dto, asString(params.get("model")))));
    }

    private AgentToolResult executeTextCorrect(Map<String, Object> params) {
        String content = firstNonBlank(asString(params.get("content")), "");
        String instruction = firstNonBlank(asString(params.get("instruction")), "请对文本进行纠错和润色");
        String prompt = """
                %s
                
                文本:
                %s
                
                请返回修改后的文本、问题说明和修改建议。
                """.formatted(instruction, content);
        String result = chatService.callChatApiWithModelCode(prompt, asString(params.get("model")));
        return AgentToolResult.success("text-correct", "文本处理完成", Map.of("result", result));
    }

    private AgentToolResult executeFileDownloadUrl(Map<String, Object> params) {
        FileDownloadDTO dto = new FileDownloadDTO();
        dto.setBucketName(asString(params.get("bucketName")));
        dto.setObjectName(requireParam(params, "objectName"));
        dto.setDirectDownload(false);
        return AgentToolResult.success("file-download-url", "下载地址生成完成", toMap(fileDownloadService.getFileUrl(dto)));
    }

    private AgentToolResult executeFileDelete(Map<String, Object> params, String userId) {
        FileDeleteDTO dto = new FileDeleteDTO();
        dto.setBucketName(asString(params.get("bucketName")));
        dto.setObjectName(requireParam(params, "objectName"));
        dto.setRequireConfirmation(booleanValue(params.get("requireConfirmation"), true));
        dto.setConfirmationToken(asString(params.get("confirmationToken")));
        String deleter = Boolean.FALSE.equals(dto.getRequireConfirmation())
                ? "agent-approved:" + valueOrDefault(userId, "agent")
                : valueOrDefault(userId, "agent");
        return AgentToolResult.success("file-delete", "删除请求处理完成", toMap(fileDeleteService.deleteFile(dto, deleter)));
    }

    private AgentToolResult executeFileRestore(Map<String, Object> params) {
        FileRestoreDTO dto = new FileRestoreDTO();
        dto.setRecycleId(requireParam(params, "recycleId"));
        dto.setBucketName(asString(params.get("bucketName")));
        dto.setNewObjectName(asString(params.get("newObjectName")));
        return AgentToolResult.success("file-restore", "文件恢复完成", toMap(fileDeleteService.restoreFile(dto)));
    }

    private AgentToolResult executeFileVersionList(Map<String, Object> params) {
        FileVersionDTO dto = new FileVersionDTO();
        dto.setBucketName(asString(params.get("bucketName")));
        dto.setObjectName(requireParam(params, "objectName"));
        dto.setVersionId(asString(params.get("versionId")));
        return AgentToolResult.success("file-version-list", "文件版本查询完成", toMap(fileVersionService.getVersions(dto)));
    }

    private AgentToolResult executeFileVersionSwitch(Map<String, Object> params) {
        FileVersionSwitchDTO dto = new FileVersionSwitchDTO();
        dto.setBucketName(asString(params.get("bucketName")));
        dto.setObjectName(requireParam(params, "objectName"));
        dto.setTargetVersionId(requireParam(params, "targetVersionId"));
        return AgentToolResult.success("file-version-switch", "文件版本切换完成", toMap(fileVersionService.switchVersion(dto)));
    }

    private AgentToolResult executeHtmlPpt(Map<String, Object> params) {
        Object result = skillExecutorService.executeSkill(
                "HTML PPT Skill",
                firstNonBlank(asString(params.get("outline")), ""),
                firstNonBlank(asString(params.get("theme")), "tokyo-night"),
                firstNonBlank(asString(params.get("title")), "演示文稿"),
                asString(params.get("model"))
        );
        return AgentToolResult.success("html-ppt-generate", "HTML PPT生成完成", toMap(result));
    }

    private AgentToolResult enforceDestructiveApproval(AgentPlanStep step, Map<String, Object> context) {
        AgentToolDefinition definition = toolRegistry.get(step.getToolName());
        if (definition == null || !definition.isDestructive()) {
            context.remove("confirmedAction");
            return null;
        }

        String userId = String.valueOf(context.get("userId"));
        String token = asString(step.getParams().get("agentApprovalToken"));
        AgentApprovalService.ApprovalResult result = agentApprovalService.verifyAndConsumeDetailed(token, userId, step.getToolName(), step.getParams());
        if (result == AgentApprovalService.ApprovalResult.OK) {
            context.put("confirmedAction", true);
            step.getParams().put("requireConfirmation", false);
            return null;
        }

        AgentApprovalService.ApprovalChallenge challenge =
                agentApprovalService.createChallenge(userId, step.getToolName(), step.getParams());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("agentApprovalToken", challenge.token());
        data.put("expiresAt", challenge.expiresAt());
        data.put("toolName", step.getToolName());
        data.put("params", step.getParams());
        data.put("approvalReason", result.name());
        data.put("nextStep", "确认危险操作后，将 agentApprovalToken 原样带回本接口重试。");
        String message = switch (result) {
            case MISSING -> "危险操作需要服务端二次确认";
            case EXPIRED_OR_USED -> "审批令牌已过期或已使用，请重新确认";
            case PARAMS_MISMATCH -> "审批令牌与当前参数不匹配，请重新确认";
            default -> "危险操作需要服务端二次确认";
        };
        return AgentToolResult.actionRequired(step.getToolName(), message, data);
    }

    private List<Map<String, Object>> searchKnowledge(String query, int topK, String strategy, String userId, String knowledgeBaseId) {
        Reranker.RerankStrategy rerankStrategy = parseRerankStrategy(strategy);
        int limitedTopK = Math.max(1, Math.min(topK, 20));
        return knowledgeBase.hybridSearchWithRerank(query, limitedTopK, rerankStrategy, userId, valueOrDefault(knowledgeBaseId, "default"));
    }

    private Reranker.RerankStrategy parseRerankStrategy(String strategy) {
        try {
            return Reranker.RerankStrategy.valueOf(valueOrDefault(strategy, "HYBRID").toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return Reranker.RerankStrategy.HYBRID;
        }
    }

    private String synthesizeAnswer(AgentExecutionRequest request, List<AgentPlanStep> plan,
                                    List<AgentToolResult> results, Map<String, Object> context, boolean requiresAction) {
        if (!results.isEmpty() && "direct-answer".equals(results.get(results.size() - 1).getToolName())) {
            Object answer = results.get(results.size() - 1).getData().get("answer");
            if (answer != null) {
                return answer.toString();
            }
        }

        String prompt = """
                你是DocAI Agent，请根据任务、执行计划、工具结果给用户一个清晰的最终回复。
                如果需要用户补充文件、确认token或参数，请明确说明下一步。
                
                用户任务:
                %s
                
                执行计划:
                %s
                
                工具结果:
                %s
                
                上下文:
                %s
                
                是否需要用户动作: %s
                """.formatted(request.getTask(), safeJson(plan), safeJson(results), safeJson(context), requiresAction);
        try {
            return chatService.callChatApiWithModelCode(prompt, request.getModel());
        } catch (Exception e) {
            log.warn("最终回答生成失败，使用规则总结: {}", e.getMessage());
            return fallbackSummary(results, requiresAction);
        }
    }

    private String fallbackSummary(List<AgentToolResult> results, boolean requiresAction) {
        if (results.isEmpty()) {
            return "任务已规划，但没有执行任何工具。";
        }
        AgentToolResult last = results.get(results.size() - 1);
        if (requiresAction) {
            return last.getMessage();
        }
        return last.getMessage() + "：" + safeJson(last.getData());
    }

    private String resolveStatus(List<AgentToolResult> results) {
        if (results.isEmpty()) {
            return "planned";
        }
        return results.stream().anyMatch(r -> "error".equals(r.getStatus())) ? "error" : "success";
    }

    private String buildKnowledgeContext(List<Map<String, Object>> results) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            Map<String, Object> result = results.get(i);
            builder.append("来源").append(i + 1).append(" ID=").append(result.get("id")).append("\n")
                    .append(result.getOrDefault("content", "")).append("\n\n");
        }
        return builder.toString();
    }

    private String stripJson(String raw) {
        String text = raw.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```[a-zA-Z]*\\s*", "");
            text = text.replaceFirst("\\s*```$", "");
        }
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private String stripObjectJson(String raw) {
        String text = raw.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```[a-zA-Z]*\\s*", "");
            text = text.replaceFirst("\\s*```$", "");
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private Map<String, Object> toMap(Object value) {
        if (value == null) {
            return new HashMap<>();
        }
        return objectMapper.convertValue(value, new TypeReference<>() {});
    }

    private Map<String, Object> toStringObjectMap(Map<?, ?> map) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null) {
                result.put(entry.getKey().toString(), entry.getValue());
            }
        }
        return result;
    }

    private String safeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private String requireParam(Map<String, Object> params, String key) {
        String value = asString(params.get(key));
        if (isBlank(value)) {
            throw new IllegalArgumentException("缺少必要参数: " + key);
        }
        return value;
    }

    private String extractQuotedText(String task) {
        if (task == null) {
            return null;
        }
        int left = Math.max(task.lastIndexOf('“'), task.lastIndexOf('"'));
        int right = Math.max(task.lastIndexOf('”'), task.lastIndexOf('"'));
        if (left >= 0 && right > left) {
            return task.substring(left + 1, right);
        }
        return null;
    }

    private boolean containsAny(String text, String... words) {
        if (text == null) {
            return false;
        }
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private int intValue(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value != null ? Integer.parseInt(value.toString()) : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private boolean booleanValue(Object value, boolean defaultValue) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value != null ? Boolean.parseBoolean(value.toString()) : defaultValue;
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private String valueOrDefault(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
