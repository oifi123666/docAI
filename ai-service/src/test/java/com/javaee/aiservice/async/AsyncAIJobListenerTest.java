package com.javaee.aiservice.async;

import com.javaee.aiservice.agent.ChatService;
import com.javaee.aiservice.agent.execution.AgentExecutionService;
import com.javaee.aiservice.agent.execution.model.AgentExecutionRequest;
import com.javaee.aiservice.service.AIService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

class AsyncAIJobListenerTest {

    @Test
    void handleModelJobExecutesAgentRequestAndStoresResult() {
        AsyncAIJobListener listener = new AsyncAIJobListener();
        AsyncAIJobService asyncAIJobService = mock(AsyncAIJobService.class);
        AIService aiService = mock(AIService.class);
        ChatService chatService = mock(ChatService.class);
        AgentExecutionService agentExecutionService = mock(AgentExecutionService.class);

        doAnswer(invocation -> {
            assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo("user-1");
            return Map.of("status", "success", "answer", "done");
        }).when(agentExecutionService).execute(org.mockito.ArgumentMatchers.any(AgentExecutionRequest.class));

        ReflectionTestUtils.setField(listener, "asyncAIJobService", asyncAIJobService);
        ReflectionTestUtils.setField(listener, "aiService", aiService);
        ReflectionTestUtils.setField(listener, "chatService", chatService);
        ReflectionTestUtils.setField(listener, "agentExecutionService", agentExecutionService);

        AsyncAIJobMessage message = new AsyncAIJobMessage();
        message.setJobId("job-1");
        message.setType("agent");
        message.setModel("qwen3.6-plus");
        message.setUserId("user-1");
        message.setPayload(Map.of(
                "task", "总结当前文档",
                "conversationId", "conv-1",
                "context", Map.of("documentId", "doc-1")
        ));

        listener.handleModelJob(message);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        ArgumentCaptor<AgentExecutionRequest> requestCaptor = ArgumentCaptor.forClass(AgentExecutionRequest.class);
        verify(agentExecutionService).execute(requestCaptor.capture());
        AgentExecutionRequest request = requestCaptor.getValue();
        assertThat(request.getTask()).isEqualTo("总结当前文档");
        assertThat(request.getModel()).isEqualTo("qwen3.6-plus");
        assertThat(request.getUserId()).isEqualTo("user-1");
        assertThat(request.getConversationId()).isEqualTo("conv-1");
        assertThat(request.getContext()).containsEntry("documentId", "doc-1");
        verify(asyncAIJobService).markRunning("job-1");
        verify(asyncAIJobService).markSuccess("job-1", Map.of("status", "success", "answer", "done"));
    }
}
