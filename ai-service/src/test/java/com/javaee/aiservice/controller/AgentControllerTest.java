package com.javaee.aiservice.controller;

import com.javaee.aiservice.agent.KnowledgeIndexAgent;
import com.javaee.aiservice.agent.execution.AgentExecutionService;
import com.javaee.aiservice.agent.execution.task.AgentTaskRegistry;
import com.javaee.aiservice.conversation.ConversationManager;
import com.javaee.aiservice.security.RequestUserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AgentControllerTest {

    private MockMvc mockMvc;
    private AgentTaskRegistry taskRegistry;
    private KnowledgeIndexAgent knowledgeIndexAgent;
    private AgentExecutionService agentExecutionService;
    private RequestUserContext requestUserContext;
    private ConversationManager conversationManager;

    @BeforeEach
    void setUp() {
        AgentController controller = new AgentController();
        taskRegistry = mock(AgentTaskRegistry.class);
        knowledgeIndexAgent = mock(KnowledgeIndexAgent.class);
        agentExecutionService = mock(AgentExecutionService.class);
        requestUserContext = mock(RequestUserContext.class);
        conversationManager = mock(ConversationManager.class);

        when(requestUserContext.getRequiredUserId()).thenReturn("user-1");
        when(requestUserContext.getCurrentRole()).thenReturn("user");
        when(requestUserContext.isAdmin()).thenReturn(false);

        ReflectionTestUtils.setField(controller, "agentTaskRegistry", taskRegistry);
        ReflectionTestUtils.setField(controller, "knowledgeIndexAgent", knowledgeIndexAgent);
        ReflectionTestUtils.setField(controller, "agentExecutionService", agentExecutionService);
        ReflectionTestUtils.setField(controller, "requestUserContext", requestUserContext);
        ReflectionTestUtils.setField(controller, "conversationManager", conversationManager);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void listTasksReturnsCurrentUserSnapshots() throws Exception {
        when(taskRegistry.listByUser("user-1")).thenReturn(List.of(Map.of(
                "traceId", "trace-1",
                "userId", "user-1",
                "status", "success"
        )));

        mockMvc.perform(get("/api/ai/agent/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].traceId").value("trace-1"));
    }

    @Test
    void cancelTaskRejectsOtherUsersSnapshot() throws Exception {
        when(taskRegistry.get("trace-2")).thenReturn(Map.of(
                "traceId", "trace-2",
                "userId", "other",
                "status", "running"
        ));

        assertThatThrownBy(() -> mockMvc.perform(post("/api/ai/agent/tasks/trace-2/cancel")))
                .hasRootCauseInstanceOf(SecurityException.class)
                .hasMessageContaining("无权取消该任务");
    }

    @Test
    void retryStepDelegatesToExecutionService() throws Exception {
        when(taskRegistry.get("trace-1")).thenReturn(Map.of(
                "traceId", "trace-1",
                "userId", "user-1",
                "status", "error"
        ));
        when(agentExecutionService.retryStep(eq("trace-1"), eq("step-1"), any()))
                .thenReturn(Map.of("traceId", "trace-1", "stepId", "step-1", "status", "success"));

        mockMvc.perform(post("/api/ai/agent/tasks/trace-1/steps/step-1/retry")
                        .contentType("application/json")
                        .content("{\"query\":\"new\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stepId").value("step-1"))
                .andExpect(jsonPath("$.data.status").value("success"));
    }

    @Test
    void listKnowledgeJobsFiltersCurrentUser() throws Exception {
        when(knowledgeIndexAgent.listJobs("user-1", "kb-1")).thenReturn(List.of(Map.of(
                "jobId", "job-1",
                "documentId", "doc-1",
                "userId", "user-1",
                "knowledgeBaseId", "kb-1",
                "status", "INDEXED"
        )));

        mockMvc.perform(get("/api/ai/agent/knowledge/jobs").param("knowledgeBaseId", "kb-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].jobId").value("job-1"));
    }

    @Test
    void deleteKnowledgeJobRejectsOtherUsersJob() throws Exception {
        when(knowledgeIndexAgent.getJobStatus("job-2")).thenReturn(Map.of(
                "jobId", "job-2",
                "userId", "other",
                "status", "INDEXED"
        ));

        assertThatThrownBy(() -> mockMvc.perform(delete("/api/ai/agent/knowledge/jobs/job-2")))
                .hasRootCauseInstanceOf(SecurityException.class)
                .hasMessageContaining("无权删除该任务");
    }

    @Test
    void workbenchOverviewIncludesTasksAndKnowledgeJobs() throws Exception {
        when(agentExecutionService.listTools()).thenReturn(List.of());
        when(conversationManager.getUserConversations("user-1")).thenReturn(List.of());
        when(taskRegistry.listByUser("user-1")).thenReturn(List.of(Map.of("traceId", "trace-1")));
        when(knowledgeIndexAgent.listJobs("user-1", null)).thenReturn(List.of(Map.of("jobId", "job-1")));

        mockMvc.perform(get("/api/ai/agent/workbench/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tasks", hasSize(1)))
                .andExpect(jsonPath("$.data.knowledgeJobs", hasSize(1)));
    }
}
