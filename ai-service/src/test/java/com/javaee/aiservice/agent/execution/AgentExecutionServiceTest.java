package com.javaee.aiservice.agent.execution;

import com.javaee.aiservice.agent.ChatService;
import com.javaee.aiservice.agent.execution.approval.AgentApprovalService;
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
import com.javaee.aiservice.internal.InternalService;
import com.javaee.aiservice.rag.KnowledgeBase;
import com.javaee.aiservice.rag.Reranker;
import com.javaee.aiservice.security.RequestUserContext;
import com.javaee.aiservice.service.FileDeleteService;
import com.javaee.aiservice.vo.FileDeleteVO;
import com.javaee.aiservice.vo.FileRestoreVO;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentExecutionServiceTest {

    @Test
    void toolRegistryExposesRequiredSchemaAndRiskLevel() {
        AgentToolRegistry registry = new AgentToolRegistry();

        AgentToolDefinition deleteTool = registry.get("file-delete");
        AgentToolDefinition restoreTool = registry.get("file-restore");

        assertThat(deleteTool).isNotNull();
        assertThat(deleteTool.isDestructive()).isFalse();
        assertThat(deleteTool.getRiskLevel()).isEqualTo("low");
        assertThat(deleteTool.getParameterSchema().get("objectName").isRequired()).isFalse();
        assertThat(deleteTool.getParameterSchema().get("documentId").isRequired()).isFalse();
        assertThat(deleteTool.getParameterSchema()).doesNotContainKeys("requireConfirmation", "confirmationToken");
        assertThat(restoreTool).isNotNull();
        assertThat(restoreTool.isDestructive()).isTrue();
        assertThat(registry.get("file-upload")).isNull();

        AgentToolDefinition documentRead = registry.get("document-read");
        AgentToolDefinition documentWrite = registry.get("document-write");
        AgentToolDefinition textToFile = registry.get("text-to-file");
        assertThat(documentRead).isNotNull();
        assertThat(documentRead.getParameterSchema().get("documentId").isRequired()).isTrue();
        assertThat(documentWrite).isNotNull();
        assertThat(documentWrite.getParameterSchema().get("documentId").isRequired()).isFalse();
        assertThat(documentWrite.getParameterSchema().get("content").isRequired()).isTrue();
        assertThat(documentWrite.getParameterSchema()).containsKey("insertAfterText");
        assertThat(documentWrite.getDescription()).contains("前端");
        assertThat(textToFile).isNotNull();
        assertThat(textToFile.getParameterSchema().get("objectName").isRequired()).isFalse();
        assertThat(textToFile.getParameterSchema().get("content").isRequired()).isTrue();
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
        assertThat(missingParameters).containsExactly("documentId/objectName");
        assertThat(toolResults.get(0).getData())
                .containsEntry("interactionType", "user_input")
                .containsEntry("resumeMode", "continue_trace");
        assertThat(response).containsKey("pendingUserInput");
        assertThat(response).doesNotContainKey("pendingApproval");
    }

    @SuppressWarnings("unchecked")
    @Test
    void continueTraceResumesMissingParameterStepWithoutApprovalToken() {
        AgentExecutionService service = new AgentExecutionService();
        AgentToolRegistry registry = new AgentToolRegistry();
        ChatService chatService = mock(ChatService.class);
        ConversationManager conversationManager = mock(ConversationManager.class);
        ContextManager contextManager = mock(ContextManager.class);
        InternalService internalService = mock(InternalService.class);
        FileDeleteService fileDeleteService = mock(FileDeleteService.class);
        RequestUserContext requestUserContext = mock(RequestUserContext.class);
        AgentTaskRegistry taskRegistry = new AgentTaskRegistry();

        when(requestUserContext.getRequiredUserId()).thenReturn("user-1");
        when(requestUserContext.getCurrentRole()).thenReturn("USER");
        when(conversationManager.createConversation("user-1")).thenReturn("conv-1");
        when(contextManager.getContext("conv-1")).thenReturn(new HashMap<>());
        when(chatService.callChatApiWithModelCode(any(), any())).thenThrow(new RuntimeException("planner unavailable"));
        when(internalService.hasPermission(eq("file-delete"), any())).thenReturn(true);
        when(fileDeleteService.deleteFile(any(FileDeleteDTO.class), eq("agent-approved:user-1")))
                .thenReturn(new FileDeleteVO("deleted", null, null, null, "删除成功"));
        doNothing().when(conversationManager).addMessageForUser(eq("conv-1"), eq("user-1"), any(), any());
        doNothing().when(contextManager).updateContext(eq("conv-1"), any());
        doNothing().when(internalService).logAudit(any(), any(), any());

        ReflectionTestUtils.setField(service, "chatService", chatService);
        ReflectionTestUtils.setField(service, "conversationManager", conversationManager);
        ReflectionTestUtils.setField(service, "contextManager", contextManager);
        ReflectionTestUtils.setField(service, "internalService", internalService);
        ReflectionTestUtils.setField(service, "toolRegistry", registry);
        ReflectionTestUtils.setField(service, "fileDeleteService", fileDeleteService);
        ReflectionTestUtils.setField(service, "requestUserContext", requestUserContext);
        ReflectionTestUtils.setField(service, "taskRegistry", taskRegistry);

        AgentExecutionRequest request = new AgentExecutionRequest();
        request.setTask("删除文件");
        request.setContext(Map.of());
        request.setReflectionEnabled(false);
        request.setAutoReplan(false);

        Map<String, Object> first = service.execute(request);
        assertThat(first.get("status")).isEqualTo("action_required");
        assertThat(first).containsKey("pendingUserInput");
        assertThat(first).doesNotContainKey("pendingApproval");

        AgentExecutionRequest resumed = new AgentExecutionRequest();
        resumed.setTask("文件是 a.txt");
        resumed.setContinueTraceId(first.get("traceId").toString());
        resumed.setContext(Map.of("objectName", "a.txt", "bucketName", "doc-ai"));
        resumed.setReflectionEnabled(false);
        resumed.setAutoReplan(false);

        Map<String, Object> result = service.execute(resumed);

        assertThat(result.get("status")).isEqualTo("success");
        assertThat(result).doesNotContainKey("pendingUserInput");
        List<AgentToolResult> toolResults = (List<AgentToolResult>) result.get("toolResults");
        assertThat(toolResults).extracting(AgentToolResult::getToolName).containsExactly("file-delete");
        assertThat(toolResults).extracting(AgentToolResult::getStatus).containsExactly("success");
        verify(fileDeleteService).deleteFile(
                argThat(dto -> "a.txt".equals(dto.getObjectName()) && "doc-ai".equals(dto.getBucketName())),
                eq("agent-approved:user-1"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void executeFreezesCurrentIterationWhenStepRequiresAction() {
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
        when(internalService.hasPermission(any(), any())).thenReturn(true);
        when(chatService.callChatApiWithModelCode(any(), any()))
                .thenReturn("""
                        [
                          {"id":"ask","description":"先问用户","toolName":"ask-user","params":{"question":"请补充目标文件"}},
                          {"id":"answer","description":"后续回答","toolName":"direct-answer","params":{"question":"不应在同一轮提前执行"}}
                        ]
                        """, "请补充目标文件");
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
        request.setTask("先问用户再回答");
        request.setContext(Map.of());
        request.setReflectionEnabled(false);
        request.setAutoReplan(false);

        Map<String, Object> response = service.execute(request);

        assertThat(response.get("status")).isEqualTo("action_required");
        assertThat(response.get("stoppedReason")).isEqualTo("action_required");

        List<AgentToolResult> toolResults = (List<AgentToolResult>) response.get("toolResults");
        assertThat(toolResults).hasSize(1);
        assertThat(toolResults.get(0).getToolName()).isEqualTo("ask-user");

        List<AgentPlanStep> plan = (List<AgentPlanStep>) response.get("plan");
        assertThat(plan).hasSize(2);
        assertThat(plan.get(0).getStatus()).isEqualTo(AgentStepStatus.WAITING_USER.value());
        assertThat(plan.get(1).getToolName()).isEqualTo("direct-answer");
        assertThat(plan.get(1).getStatus()).isEqualTo(AgentStepStatus.PENDING.value());
    }

    @SuppressWarnings("unchecked")
    @Test
    void confirmApprovalContinuesFrozenPlanInsteadOfReturningSingleToolResult() {
        AgentExecutionService service = new AgentExecutionService();
        AgentToolRegistry registry = new AgentToolRegistry();
        ChatService chatService = mock(ChatService.class);
        ConversationManager conversationManager = mock(ConversationManager.class);
        ContextManager contextManager = mock(ContextManager.class);
        InternalService internalService = mock(InternalService.class);
        FileDeleteService fileDeleteService = mock(FileDeleteService.class);
        RequestUserContext requestUserContext = mock(RequestUserContext.class);
        AgentApprovalService agentApprovalService = mock(AgentApprovalService.class);
        AgentTaskRegistry taskRegistry = new AgentTaskRegistry();

        when(requestUserContext.getRequiredUserId()).thenReturn("user-1");
        when(requestUserContext.getCurrentRole()).thenReturn("USER");
        when(conversationManager.createConversation("user-1")).thenReturn("conv-1");
        when(contextManager.getContext("conv-1")).thenReturn(new HashMap<>());
        when(internalService.hasPermission(any(), any())).thenReturn(true);
        when(chatService.callChatApiWithModelCode(any(), any()))
                .thenReturn("""
                        [
                          {"id":"restore","description":"恢复文件","toolName":"file-restore","params":{"recycleId":"rid-1","bucketName":"doc-ai"}},
                          {"id":"answer","description":"告知用户结果","toolName":"direct-answer","params":{"question":"恢复完成后告诉用户"},"dependsOn":["restore"]}
                        ]
                        """, "请确认恢复操作", "恢复已完成");
        when(agentApprovalService.verifyAndConsumeDetailed(any(), eq("user-1"), eq("file-restore"), any()))
                .thenReturn(AgentApprovalService.ApprovalResult.MISSING);
        when(agentApprovalService.verifyAndConsumeDetailed(eq("tok-restore"), eq("user-1"), eq("file-restore"), any()))
                .thenReturn(AgentApprovalService.ApprovalResult.OK);
        when(agentApprovalService.createChallenge(eq("user-1"), eq("file-restore"), any()))
                .thenReturn(new AgentApprovalService.ApprovalChallenge("tok-restore", 123456L));
        when(fileDeleteService.restoreFile(any()))
                .thenReturn(new FileRestoreVO("restored", "http://file", "doc-ai", "a.txt", "恢复成功"));
        doNothing().when(conversationManager).addMessageForUser(eq("conv-1"), eq("user-1"), any(), any());
        doNothing().when(contextManager).updateContext(eq("conv-1"), any());
        doNothing().when(internalService).logAudit(any(), any(), any());

        ReflectionTestUtils.setField(service, "chatService", chatService);
        ReflectionTestUtils.setField(service, "conversationManager", conversationManager);
        ReflectionTestUtils.setField(service, "contextManager", contextManager);
        ReflectionTestUtils.setField(service, "internalService", internalService);
        ReflectionTestUtils.setField(service, "toolRegistry", registry);
        ReflectionTestUtils.setField(service, "fileDeleteService", fileDeleteService);
        ReflectionTestUtils.setField(service, "requestUserContext", requestUserContext);
        ReflectionTestUtils.setField(service, "agentApprovalService", agentApprovalService);
        ReflectionTestUtils.setField(service, "taskRegistry", taskRegistry);

        AgentExecutionRequest request = new AgentExecutionRequest();
        request.setTask("恢复文件后告诉我结果");
        request.setContext(Map.of());
        request.setReflectionEnabled(false);
        request.setAutoReplan(false);

        Map<String, Object> firstResponse = service.execute(request);
        assertThat(firstResponse.get("status")).isEqualTo("action_required");
        assertThat(firstResponse).containsKey("pendingApproval");

        AgentExecutionRequest confirm = new AgentExecutionRequest();
        confirm.setTask("确认恢复");
        confirm.setApprovalToken("tok-restore");
        confirm.setReflectionEnabled(false);
        confirm.setAutoReplan(false);

        Map<String, Object> resumed = service.confirmApproval(confirm);

        assertThat(resumed.get("status")).isEqualTo("success");
        assertThat(resumed.get("stoppedReason")).isEqualTo("completed");
        assertThat(resumed).doesNotContainKey("pendingApproval");
        assertThat(resumed.get("answer")).isEqualTo("恢复已完成");

        List<AgentToolResult> toolResults = (List<AgentToolResult>) resumed.get("toolResults");
        assertThat(toolResults).extracting(AgentToolResult::getToolName)
                .containsExactly("file-restore", "direct-answer");
        assertThat(toolResults).extracting(AgentToolResult::getStatus)
                .containsExactly("success", "success");
    }

    @SuppressWarnings("unchecked")
    @Test
    void reflectionWithoutRevisedPlanFallsBackToFollowUpPlanner() {
        AgentExecutionService service = new AgentExecutionService();
        AgentToolRegistry registry = new AgentToolRegistry();
        ChatService chatService = mock(ChatService.class);
        ConversationManager conversationManager = mock(ConversationManager.class);
        ContextManager contextManager = mock(ContextManager.class);
        InternalService internalService = mock(InternalService.class);
        RequestUserContext requestUserContext = mock(RequestUserContext.class);
        AgentReflectionService reflectionService = mock(AgentReflectionService.class);
        KnowledgeBase knowledgeBase = mock(KnowledgeBase.class);

        when(requestUserContext.getRequiredUserId()).thenReturn("user-1");
        when(requestUserContext.getCurrentRole()).thenReturn("USER");
        when(conversationManager.createConversation("user-1")).thenReturn("conv-1");
        when(contextManager.getContext("conv-1")).thenReturn(new HashMap<>());
        when(internalService.hasPermission(any(), any())).thenReturn(true);
        when(knowledgeBase.hybridSearchWithRerank(eq("查找合同"), eq(5), eq(Reranker.RerankStrategy.HYBRID),
                eq("user-1"), eq("default"))).thenReturn(List.of(Map.of("id", "doc-1", "content", "合同片段")));
        when(chatService.callChatApiWithModelCode(any(), any()))
                .thenReturn("""
                        [{"id":"search","description":"先检索","toolName":"rag-search","params":{"query":"查找合同"}}]
                        """, """
                        [{"id":"answer","description":"根据检索结果回答","toolName":"direct-answer","params":{"question":"${steps.search.data.results}"}}]
                        """, "合同结果");
        AgentReflection reflection = new AgentReflection();
        reflection.setComplete(false);
        reflection.setContinueExecution(true);
        reflection.setRequiresReplan(true);
        reflection.setReason("需要根据检索结果继续回答，但未提供 revisedPlan");
        when(reflectionService.reflect(any(), any(), any(), any(), eq(1), eq(2))).thenReturn(reflection);

        doNothing().when(conversationManager).addMessageForUser(eq("conv-1"), eq("user-1"), any(), any());
        doNothing().when(contextManager).updateContext(eq("conv-1"), any());
        doNothing().when(internalService).logAudit(any(), any(), any());

        ReflectionTestUtils.setField(service, "chatService", chatService);
        ReflectionTestUtils.setField(service, "conversationManager", conversationManager);
        ReflectionTestUtils.setField(service, "contextManager", contextManager);
        ReflectionTestUtils.setField(service, "internalService", internalService);
        ReflectionTestUtils.setField(service, "toolRegistry", registry);
        ReflectionTestUtils.setField(service, "requestUserContext", requestUserContext);
        ReflectionTestUtils.setField(service, "reflectionService", reflectionService);
        ReflectionTestUtils.setField(service, "knowledgeBase", knowledgeBase);
        ReflectionTestUtils.setField(service, "taskRegistry", new AgentTaskRegistry());

        AgentExecutionRequest request = new AgentExecutionRequest();
        request.setTask("查找合同");
        request.setContext(Map.of("userId", "user-1"));
        request.setReflectionEnabled(true);
        request.setAutoReplan(true);
        request.setMaxIterations(2);

        Map<String, Object> response = service.execute(request);

        assertThat(response.get("status")).isEqualTo("success");
        assertThat(response.get("answer")).isEqualTo("合同结果");
        assertThat(response.get("stoppedReason")).isEqualTo("completed");
        List<AgentToolResult> toolResults = (List<AgentToolResult>) response.get("toolResults");
        assertThat(toolResults).extracting(AgentToolResult::getToolName)
                .containsExactly("rag-search", "direct-answer");
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
        assertThat(ragSearch.getParameterSchema()).containsKey("rerankStrategy");
        assertThat(ragSearch.getParameterSchema()).doesNotContainKey("strategy");
        AgentToolParameterDefinition strategy = ragSearch.getParameterSchema().get("rerankStrategy");
        assertThat(strategy.getDescription()).contains("重排序");
        assertThat(strategy.getAllowedValues()).contains("HYBRID", "VECTOR", "BM25");
        AgentToolParameterDefinition topK = ragSearch.getParameterSchema().get("topK");
        assertThat(topK.getMinValue().intValue()).isEqualTo(1);
        assertThat(topK.getMaxValue().intValue()).isEqualTo(50);
    }

    @Test
    void plannerPromptsDocumentStructuredFieldsAndPlaceholderContract() {
        AgentExecutionService service = new AgentExecutionService();
        ReflectionTestUtils.setField(service, "toolRegistry", new AgentToolRegistry());

        AgentExecutionRequest request = new AgentExecutionRequest();
        request.setTask("先检索再总结");

        String initialPrompt = ReflectionTestUtils.invokeMethod(
                service, "buildPlannerPrompt", request, Map.of("documentId", "doc-1"));
        String followUpPrompt = ReflectionTestUtils.invokeMethod(
                service, "buildFollowUpPlannerPrompt", request, Map.of(), List.of(), List.of(), 2);

        assertThat(initialPrompt).contains("dependsOn", "successCriteria", "retryPolicy", "maxRetries");
        assertThat(initialPrompt).contains("${steps.<id>.data.<key>}", "${steps.<id>.observation}");
        assertThat(initialPrompt).contains("rerankStrategy", "不要把它当成业务检索策略");
        assertThat(followUpPrompt).contains("dependsOn", "successCriteria", "${steps.<id>.data.<key>}");
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
    void executeFileDeleteRunsDirectlyWhenObjectNameIsKnown() {
        AgentExecutionService service = new AgentExecutionService();
        AgentToolRegistry registry = new AgentToolRegistry();
        ChatService chatService = mock(ChatService.class);
        ConversationManager conversationManager = mock(ConversationManager.class);
        ContextManager contextManager = mock(ContextManager.class);
        InternalService internalService = mock(InternalService.class);
        FileDeleteService fileDeleteService = mock(FileDeleteService.class);
        RequestUserContext requestUserContext = mock(RequestUserContext.class);

        when(requestUserContext.getRequiredUserId()).thenReturn("user-1");
        when(requestUserContext.getCurrentRole()).thenReturn("USER");
        when(conversationManager.createConversation("user-1")).thenReturn("conv-1");
        when(contextManager.getContext("conv-1")).thenReturn(new HashMap<>());
        when(chatService.callChatApiWithModelCode(any(), any())).thenThrow(new RuntimeException("planner unavailable"));
        when(internalService.hasPermission(eq("file-delete"), any())).thenReturn(true);
        when(fileDeleteService.deleteFile(any(FileDeleteDTO.class), eq("agent-approved:user-1")))
                .thenReturn(new FileDeleteVO("recycle", null, null, "rid-1", "已移入回收站"));
        doNothing().when(conversationManager).addMessageForUser(eq("conv-1"), eq("user-1"), any(), any());
        doNothing().when(contextManager).updateContext(eq("conv-1"), any());
        doNothing().when(internalService).logAudit(any(), any(), any());

        ReflectionTestUtils.setField(service, "chatService", chatService);
        ReflectionTestUtils.setField(service, "conversationManager", conversationManager);
        ReflectionTestUtils.setField(service, "contextManager", contextManager);
        ReflectionTestUtils.setField(service, "internalService", internalService);
        ReflectionTestUtils.setField(service, "toolRegistry", registry);
        ReflectionTestUtils.setField(service, "fileDeleteService", fileDeleteService);
        ReflectionTestUtils.setField(service, "requestUserContext", requestUserContext);
        ReflectionTestUtils.setField(service, "taskRegistry", new AgentTaskRegistry());

        AgentExecutionRequest request = new AgentExecutionRequest();
        request.setTask("删除文件");
        request.setContext(Map.of(
                "objectName", "79dd2c3f-demo.docx",
                "bucketName", "doc-ai-real"));
        request.setReflectionEnabled(false);

        Map<String, Object> result = service.execute(request);

        assertThat(result.get("status")).isEqualTo("success");
        assertThat(result).doesNotContainKey("pendingApproval");
        verify(fileDeleteService).deleteFile(
                argThat(dto -> "79dd2c3f-demo.docx".equals(dto.getObjectName())
                        && "doc-ai-real".equals(dto.getBucketName())
                        && Boolean.FALSE.equals(dto.getRequireConfirmation())),
                eq("agent-approved:user-1"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void fallbackPlanExtractsUnquotedObjectNameForFileDelete() {
        AgentExecutionService service = new AgentExecutionService();
        ReflectionTestUtils.setField(service, "toolRegistry", new AgentToolRegistry());

        AgentExecutionRequest request = new AgentExecutionRequest();
        request.setTask("删除文件6d96d090927b45118bd722519dabdce2.pdf");

        List<AgentPlanStep> plan = ReflectionTestUtils.invokeMethod(
                service, "fallbackPlan", request, Map.of("bucketName", "doc-ai"));

        assertThat(plan).hasSize(1);
        assertThat(plan.get(0).getToolName()).isEqualTo("file-delete");
        assertThat(plan.get(0).getParams())
                .containsEntry("objectName", "6d96d090927b45118bd722519dabdce2.pdf")
                .containsEntry("bucketName", "doc-ai")
                .containsEntry("requireConfirmation", false);
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

    @SuppressWarnings("unchecked")
    @Test
    void fallbackPlanUsesFrontendWritePatchWhenDocumentIdExists() {
        AgentExecutionService service = new AgentExecutionService();
        ReflectionTestUtils.setField(service, "toolRegistry", new AgentToolRegistry());

        AgentExecutionRequest request = new AgentExecutionRequest();
        request.setTask("把这个文档进行内容扩写");
        request.setKnowledgeBaseId("kb-1");

        List<AgentPlanStep> plan = ReflectionTestUtils.invokeMethod(
                service, "fallbackPlan", request, Map.of("documentId", "doc-1", "writeMode", "append"));

        assertThat(plan).hasSize(2);
        assertThat(plan).extracting(AgentPlanStep::getToolName)
                .containsExactly("direct-answer", "document-write");
        assertThat(plan.get(1).getParams())
                .containsEntry("documentId", "doc-1")
                .containsEntry("content", "${answer}")
                .containsEntry("writeMode", "append")
                .containsEntry("knowledgeBaseId", "kb-1");
    }

    @Test
    void fallbackPlanUsesDocumentIdForDeleteInsteadOfFrontendWrite() {
        AgentExecutionService service = new AgentExecutionService();
        ReflectionTestUtils.setField(service, "toolRegistry", new AgentToolRegistry());

        AgentExecutionRequest request = new AgentExecutionRequest();
        request.setTask("删除当前前端文档对应的文件");

        List<AgentPlanStep> plan = ReflectionTestUtils.invokeMethod(
                service, "fallbackPlan", request, Map.of("documentId", "doc-1", "userId", "7"));

        assertThat(plan).hasSize(1);
        assertThat(plan.get(0).getToolName()).isEqualTo("file-delete");
        assertThat(plan.get(0).getParams())
                .containsEntry("documentId", "doc-1")
                .containsEntry("requireConfirmation", false);
    }

    @SuppressWarnings("unchecked")
    @Test
    void fallbackPlanKeepsObjectNameCompatibilityPath() {
        AgentExecutionService service = new AgentExecutionService();
        ReflectionTestUtils.setField(service, "toolRegistry", new AgentToolRegistry());

        AgentExecutionRequest request = new AgentExecutionRequest();
        request.setTask("生成一段说明并写入文件");

        List<AgentPlanStep> plan = ReflectionTestUtils.invokeMethod(
                service, "fallbackPlan", request, Map.of("objectName", "report.txt", "bucketName", "documents"));

        assertThat(plan).hasSize(2);
        assertThat(plan).extracting(AgentPlanStep::getToolName)
                .containsExactly("direct-answer", "text-to-file");
        assertThat(plan.get(1).getParams())
                .containsEntry("content", "${answer}")
                .containsEntry("objectName", "report.txt")
                .containsEntry("bucketName", "documents");
    }

    @Test
    void fallbackPlanDefaultsCompatibilityFileWriteToUserBucket() {
        AgentExecutionService service = new AgentExecutionService();
        ReflectionTestUtils.setField(service, "toolRegistry", new AgentToolRegistry());

        AgentExecutionRequest request = new AgentExecutionRequest();
        request.setTask("生成一段说明并写入文件");
        request.setUserId("42");

        List<AgentPlanStep> plan = ReflectionTestUtils.invokeMethod(
                service, "fallbackPlan", request, Map.of("objectName", "report.txt"));

        assertThat(plan).hasSize(2);
        assertThat(plan.get(1).getParams())
                .containsEntry("objectName", "report.txt")
                .containsEntry("bucketName", "user-42");
    }

    @Test
    void documentWriteMissingGeneratedContentRequiresAction() {
        AgentExecutionService service = new AgentExecutionService();
        ReflectionTestUtils.setField(service, "toolRegistry", new AgentToolRegistry());

        AgentExecutionRequest request = new AgentExecutionRequest();
        request.setTask("把这个文档进行内容扩写");
        AgentPlanStep step = new AgentPlanStep("step-1", "写回文档", "document-write", new HashMap<>());
        step.getParams().put("documentId", "doc-1");

        ReflectionTestUtils.invokeMethod(service, "fillDefaultParams", step, request, Map.of("documentId", "doc-1"));
        AgentToolResult validation = ReflectionTestUtils.invokeMethod(service, "validateToolParameters", step);

        assertThat(step.getParams()).doesNotContainKey("content");
        assertThat(validation).isNotNull();
        assertThat(validation.getStatus()).isEqualTo("action_required");
        assertThat(validation.getData()).containsEntry("missingParameters", List.of("content"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void documentWriteReturnsFrontendPatchWithoutPersisting() {
        AgentExecutionService service = new AgentExecutionService();

        AgentExecutionRequest request = new AgentExecutionRequest();
        request.setKnowledgeBaseId("kb-1");
        request.setUserId("user-1");
        Map<String, Object> params = new HashMap<>();
        params.put("documentId", "doc-1");
        params.put("content", "扩写内容");
        params.put("writeMode", "append");
        params.put("knowledgeBaseId", "kb-1");

        AgentToolResult result = ReflectionTestUtils.invokeMethod(
                service, "executeDocumentWrite", params, request, Map.of("userId", "user-1"));

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("success");
        assertThat(result.getData()).containsEntry("documentId", "doc-1")
                .containsEntry("writeMode", "append")
                .containsEntry("result", "frontend-document-write")
                .containsEntry("requiresFrontendWrite", true)
                .containsEntry("persisted", false)
                .containsEntry("content", "扩写内容");
    }

    @Test
    void documentWriteParsesStructuredPayloadAndCleansMarkdown() {
        AgentExecutionService service = new AgentExecutionService();

        AgentExecutionRequest request = new AgentExecutionRequest();
        Map<String, Object> params = new HashMap<>();
        params.put("documentId", "doc-1");
        params.put("content", """
                {"content":"**补充说明**\\n* 设 $P$ 为数域 $\\\\mathbb{C}$ 的子集。","writeMode":"insert","insertAfterText":"**1. 数域**","changeLog":"扩写数域","contentFormat":"plain_text"}
                """);

        AgentToolResult result = ReflectionTestUtils.invokeMethod(
                service, "executeDocumentWrite", params, request, Map.of("userId", "user-1"));

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("success");
        assertThat(result.getData()).containsEntry("writeMode", "insert")
                .containsEntry("insertAfterText", "1. 数域")
                .containsEntry("changeLog", "扩写数域");
        assertThat(result.getData().get("content").toString())
                .doesNotContain("**")
                .doesNotContain("$")
                .contains("补充说明")
                .contains("P 为数域 C 的子集");
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
        snap2.put("pendingApproval", Map.of("agentApprovalToken", "tok-pending"));
        registry.save("trace-2", snap2);

        assertThat(registry.get("trace-1")).containsEntry("userId", "alice");
        assertThat(registry.listByUser("alice")).hasSize(1);
        assertThat(registry.listByUser("bob")).hasSize(1);
        assertThat(registry.listAll()).hasSize(2);
        assertThat(registry.findByApprovalToken("tok-pending")).containsEntry("userId", "bob");

        // 终态任务无法取消
        assertThat(registry.cancel("trace-1")).isFalse();
        assertThat(registry.cancel("trace-2")).isTrue();
        assertThat(registry.isCancelled("trace-2")).isTrue();
        assertThat(registry.delete("trace-2")).isTrue();
        assertThat(registry.get("trace-2")).isEmpty();
    }
}
