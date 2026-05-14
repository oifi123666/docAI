package com.javaee.aiservice.agent.execution;

import com.javaee.aiservice.agent.ChatService;
import com.javaee.aiservice.agent.execution.approval.AgentApprovalService;
import com.javaee.aiservice.agent.execution.model.AgentExecutionRequest;
import com.javaee.aiservice.agent.execution.model.AgentPlanStep;
import com.javaee.aiservice.agent.execution.model.AgentStepStatus;
import com.javaee.aiservice.agent.execution.model.AgentToolResult;
import com.javaee.aiservice.agent.execution.task.AgentTaskRegistry;
import com.javaee.aiservice.agent.execution.tool.AgentToolDefinition;
import com.javaee.aiservice.agent.execution.tool.AgentToolParameterDefinition;
import com.javaee.aiservice.agent.execution.tool.AgentToolRegistry;
import com.javaee.aiservice.conversation.ContextManager;
import com.javaee.aiservice.conversation.ConversationManager;
import com.javaee.aiservice.internal.InternalService;
import com.javaee.aiservice.security.RequestUserContext;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentExecutionServiceTest {

    @Test
    void toolRegistryExposesRequiredSchemaAndRiskLevel() {
        AgentToolRegistry registry = new AgentToolRegistry();

        AgentToolDefinition deleteTool = registry.get("file-delete");

        assertThat(deleteTool).isNotNull();
        assertThat(deleteTool.isDestructive()).isTrue();
        assertThat(deleteTool.getRiskLevel()).isEqualTo("high");
        assertThat(deleteTool.getParameterSchema().get("objectName").isRequired()).isTrue();
        assertThat(deleteTool.getParameterSchema().get("requireConfirmation").getType()).isEqualTo("boolean");
    }

    @Test
    void executeReturnsActionRequiredWhenRequiredToolParameterIsMissing() {
        AgentExecutionService service = new AgentExecutionService();
        AgentToolRegistry registry = new AgentToolRegistry();
        ChatService chatService = mock(ChatService.class);
        ConversationManager conversationManager = mock(ConversationManager.class);
        ContextManager contextManager = mock(ContextManager.class);
        InternalService internalService = mock(InternalService.class);
        RequestUserContext requestUserContext = mock(RequestUserContext.class);

        when(requestUserContext.getRequiredUserId()).thenReturn("user-1");
        when(requestUserContext.getCurrentRole()).thenReturn("USER");
        when(conversationManager.createConversation("user-1")).thenReturn("conv-1");
        when(contextManager.getContext("conv-1")).thenReturn(new HashMap<>());
        when(chatService.callChatApiWithModelCode(any(), any())).thenThrow(new RuntimeException("planner unavailable"));
        doNothing().when(conversationManager).addMessageForUser(eq("conv-1"), eq("user-1"), any(), any());
        doNothing().when(contextManager).updateContext(eq("conv-1"), any());
        doNothing().when(internalService).logAudit(any(), any(), any());

        ReflectionTestUtils.setField(service, "chatService", chatService);
        ReflectionTestUtils.setField(service, "conversationManager", conversationManager);
        ReflectionTestUtils.setField(service, "contextManager", contextManager);
        ReflectionTestUtils.setField(service, "internalService", internalService);
        ReflectionTestUtils.setField(service, "toolRegistry", registry);
        ReflectionTestUtils.setField(service, "requestUserContext", requestUserContext);
        ReflectionTestUtils.setField(service, "taskRegistry", new AgentTaskRegistry());

        AgentExecutionRequest request = new AgentExecutionRequest();
        request.setTask("删除文件");
        request.setContext(Map.of());
        request.setReflectionEnabled(false);

        Map<String, Object> response = service.execute(request);

        assertThat(response.get("status")).isEqualTo("action_required");
        assertThat(response.get("stoppedReason")).isEqualTo("action_required");

        @SuppressWarnings("unchecked")
        List<AgentToolResult> toolResults = (List<AgentToolResult>) response.get("toolResults");
        assertThat(toolResults).hasSize(1);
        assertThat(toolResults.get(0).getToolName()).isEqualTo("file-delete");
        assertThat(toolResults.get(0).getStatus()).isEqualTo("action_required");

        @SuppressWarnings("unchecked")
        List<String> missingParameters = (List<String>) toolResults.get(0).getData().get("missingParameters");
        assertThat(missingParameters).containsExactly("objectName");
    }

    @Test
    void toolRegistryExposesAskUserToolForClarification() {
        AgentToolRegistry registry = new AgentToolRegistry();

        AgentToolDefinition askUser = registry.get("ask-user");

        assertThat(askUser).isNotNull();
        assertThat(askUser.isRequiresUserAction()).isTrue();
        assertThat(askUser.getCategory()).isEqualTo("interaction");
        assertThat(askUser.getParameterSchema().get("question").isRequired()).isTrue();
    }

    @SuppressWarnings("unchecked")
    @Test
    void agentApprovalServiceCancelDeletesPendingToken() {
        AgentApprovalService approvalService = new AgentApprovalService();
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        when(redisTemplate.delete("agent:approval:tok-1")).thenReturn(true);
        ReflectionTestUtils.setField(approvalService, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(approvalService, "approvalExpirySeconds", 60L);

        assertThat(approvalService.cancel("tok-1")).isTrue();
        assertThat(approvalService.cancel("")).isFalse();
        assertThat(approvalService.cancel(null)).isFalse();
    }

    @Test
    void planStepDefaultsExposeRetryAndRiskFields() {
        AgentPlanStep step = new AgentPlanStep();
        step.setMaxRetries(3);
        step.setRetryPolicy("exponential");
        step.setDependsOn(List.of("step-1"));
        step.setRiskLevel("high");
        step.setRequiresApproval(true);

        assertThat(step.getMaxRetries()).isEqualTo(3);
        assertThat(step.getRetryPolicy()).isEqualTo("exponential");
        assertThat(step.getDependsOn()).containsExactly("step-1");
        assertThat(step.getRiskLevel()).isEqualTo("high");
        assertThat(step.isRequiresApproval()).isTrue();
    }

    @Test
    void toolRegistryExposesEnumAndRangeConstraintsForRagSearch() {
        AgentToolRegistry registry = new AgentToolRegistry();

        AgentToolDefinition ragSearch = registry.get("rag-search");

        assertThat(ragSearch).isNotNull();
        AgentToolParameterDefinition strategy = ragSearch.getParameterSchema().get("strategy");
        assertThat(strategy.getAllowedValues()).contains("HYBRID", "VECTOR", "BM25");
        AgentToolParameterDefinition topK = ragSearch.getParameterSchema().get("topK");
        assertThat(topK.getMinValue().intValue()).isEqualTo(1);
        assertThat(topK.getMaxValue().intValue()).isEqualTo(50);
    }

    @SuppressWarnings("unchecked")
    @Test
    void approvalServiceReportsExpiredAndMismatchAndOk() {
        AgentApprovalService approvalService = new AgentApprovalService();
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        org.springframework.data.redis.core.ValueOperations<String, Object> ops =
                mock(org.springframework.data.redis.core.ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(ops);
        ReflectionTestUtils.setField(approvalService, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(approvalService, "approvalExpirySeconds", 60L);

        assertThat(approvalService.verifyAndConsumeDetailed("", "u", "file-delete", Map.of()))
                .isEqualTo(AgentApprovalService.ApprovalResult.MISSING);

        when(ops.get("agent:approval:t1")).thenReturn(null);
        assertThat(approvalService.verifyAndConsumeDetailed("t1", "u", "file-delete", Map.of()))
                .isEqualTo(AgentApprovalService.ApprovalResult.EXPIRED_OR_USED);

        when(ops.get("agent:approval:t2")).thenReturn("not-the-fingerprint");
        assertThat(approvalService.verifyAndConsumeDetailed("t2", "u", "file-delete", Map.of("objectName", "a")))
                .isEqualTo(AgentApprovalService.ApprovalResult.PARAMS_MISMATCH);

        AgentApprovalService.ApprovalChallenge challenge =
                approvalService.createChallenge("u", "file-delete", Map.of("objectName", "a"));
        when(ops.get("agent:approval:" + challenge.token()))
                .thenReturn(extractFingerprint(approvalService, "u", "file-delete", Map.of("objectName", "a")));
        when(redisTemplate.delete("agent:approval:" + challenge.token())).thenReturn(true);
        assertThat(approvalService.verifyAndConsumeDetailed(challenge.token(), "u", "file-delete", Map.of("objectName", "a")))
                .isEqualTo(AgentApprovalService.ApprovalResult.OK);
    }

    private static String extractFingerprint(AgentApprovalService service, String userId, String tool, Map<String, Object> params) {
        return (String) ReflectionTestUtils.invokeMethod(service, "fingerprint", userId, tool, params);
    }

    @Test
    void resolvePlaceholdersFillsTaskAndStepValues() {
        AgentExecutionService service = new AgentExecutionService();

        AgentPlanStep step = new AgentPlanStep();
        step.setToolName("text-summarize");
        Map<String, Object> params = new HashMap<>();
        params.put("content", "${steps.s1.data.answer} - ${task}");
        params.put("static", "no-template");
        step.setParams(params);

        AgentExecutionRequest request = new AgentExecutionRequest();
        request.setTask("总结上一步");

        Map<String, Object> context = new HashMap<>();
        Map<String, Object> stepResults = new HashMap<>();
        Map<String, Object> snapshot = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("answer", "Hello");
        snapshot.put("data", data);
        stepResults.put("s1", snapshot);
        context.put("__stepResults__", stepResults);

        service.resolvePlaceholders(step, request, context);

        assertThat(step.getParams().get("content")).isEqualTo("Hello - 总结上一步");
        assertThat(step.getParams().get("static")).isEqualTo("no-template");
    }

    @Test
    void agentStepStatusHelpersClassifyTerminalAndSuccess() {
        assertThat(AgentStepStatus.isSuccess("success")).isTrue();
        assertThat(AgentStepStatus.isSuccess("error")).isFalse();
        assertThat(AgentStepStatus.isTerminal("success")).isTrue();
        assertThat(AgentStepStatus.isTerminal("error")).isTrue();
        assertThat(AgentStepStatus.isTerminal("blocked")).isTrue();
        assertThat(AgentStepStatus.isTerminal("running")).isFalse();
        assertThat(AgentStepStatus.WAITING_USER.value()).isEqualTo("waiting_user");
    }

    @Test
    void evaluateSuccessCriteriaReportsMissingDataAndContains() {
        AgentExecutionService service = new AgentExecutionService();

        AgentPlanStep step = new AgentPlanStep();
        step.setSuccessCriteria("contains:报告;data.answer;data.score=90");

        AgentToolResult result = AgentToolResult.success("text-summarize", "这是一份分析报告", new HashMap<>());
        result.getData().put("answer", "ok");
        result.getData().put("score", "90");

        assertThat(service.evaluateSuccessCriteria(step, result)).isNull();

        result.getData().put("score", "60");
        assertThat(service.evaluateSuccessCriteria(step, result)).isEqualTo("data.score=90");

        result.setMessage("");
        result.getData().clear();
        assertThat(service.evaluateSuccessCriteria(step, result)).isEqualTo("contains:报告");
    }

    @Test
    void reflectionRequestFlagCanDisableReflectionForCompatibility() {
        AgentExecutionRequest request = new AgentExecutionRequest();

        assertThat(request.getReflectionEnabled()).isNull();

        request.setReflectionEnabled(false);

        assertThat(request.getReflectionEnabled()).isFalse();
    }

    @Test
    void taskRegistryStoresSnapshotAndSupportsCancellationAndDeletion() {
        AgentTaskRegistry registry = new AgentTaskRegistry();
        Map<String, Object> snap = new HashMap<>();
        snap.put("status", "success");
        snap.put("userId", "alice");
        registry.save("trace-1", snap);

        Map<String, Object> snap2 = new HashMap<>();
        snap2.put("status", "running");
        snap2.put("userId", "bob");
        registry.save("trace-2", snap2);

        assertThat(registry.get("trace-1")).containsEntry("userId", "alice");
        assertThat(registry.listByUser("alice")).hasSize(1);
        assertThat(registry.listByUser("bob")).hasSize(1);
        assertThat(registry.listAll()).hasSize(2);

        // 终态任务无法取消
        assertThat(registry.cancel("trace-1")).isFalse();
        assertThat(registry.cancel("trace-2")).isTrue();
        assertThat(registry.isCancelled("trace-2")).isTrue();
        assertThat(registry.delete("trace-2")).isTrue();
        assertThat(registry.get("trace-2")).isEmpty();
    }
}
