package com.javaee.aiservice.controller;

import com.javaee.aiservice.aiops.FaultDetector;
import com.javaee.aiservice.aiops.MonitoringService;
import com.javaee.aiservice.security.RequestUserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AIOpsControllerTest {

    private MockMvc mockMvc;
    private MonitoringService monitoringService;
    private FaultDetector faultDetector;
    private RequestUserContext requestUserContext;

    @BeforeEach
    void setUp() {
        AIOpsController controller = new AIOpsController();
        monitoringService = mock(MonitoringService.class);
        faultDetector = mock(FaultDetector.class);
        requestUserContext = mock(RequestUserContext.class);

        when(requestUserContext.isAdmin()).thenReturn(false);

        ReflectionTestUtils.setField(controller, "monitoringService", monitoringService);
        ReflectionTestUtils.setField(controller, "faultDetector", faultDetector);
        ReflectionTestUtils.setField(controller, "requestUserContext", requestUserContext);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getMetricsAllowsNonAdmin() throws Exception {
        when(monitoringService.getAllMetrics()).thenReturn(Map.of("requests", 12L));

        mockMvc.perform(get("/api/ai/aiops/monitor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requests").value(12));
    }

    @Test
    void metricWriteEndpointsAllowNonAdmin() throws Exception {
        doNothing().when(monitoringService).incrementCounter("ai.requests", 2L);
        doNothing().when(monitoringService).recordTimer("ai.request", 123L);

        mockMvc.perform(post("/api/ai/aiops/metrics/counter")
                        .param("name", "ai.requests")
                        .param("delta", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/ai/aiops/metrics/timer")
                        .param("name", "ai.request")
                        .param("duration", "123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void resetMetricsStillRequiresAdmin() {
        assertThatThrownBy(() -> mockMvc.perform(post("/api/ai/aiops/metrics/reset")))
                .hasRootCauseInstanceOf(SecurityException.class)
                .hasMessageContaining("仅管理员可访问");
    }
}
