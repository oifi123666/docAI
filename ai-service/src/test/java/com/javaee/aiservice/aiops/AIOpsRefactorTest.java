package com.javaee.aiservice.aiops;

import com.javaee.aiservice.aiops.alert.AlertService;
import com.javaee.aiservice.internal.InternalService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AIOpsRefactorTest {

    @Test
    void monitoringServiceReportsWindowStats() {
        MonitoringService monitoringService = new MonitoringService();
        ReflectionTestUtils.setField(monitoringService, "retentionMs", 900_000L);

        monitoringService.incrementCounter("ai.requests", 3);
        monitoringService.incrementCounter("ai.errors", 1);
        monitoringService.recordTimer("ai.request", 100);
        monitoringService.recordTimer("ai.request", 200);
        monitoringService.recordTimer("ai.request", 300);

        Map<String, Object> metrics = monitoringService.getMetricsForWindow(60_000L);
        assertEquals(3L, metrics.get("ai.requests.count"));
        assertEquals(1L, metrics.get("ai.errors.count"));
        assertEquals(1.0 / 3, (double) metrics.get("ai.errorRate"), 0.0001);

        @SuppressWarnings("unchecked")
        Map<String, Object> stats = (Map<String, Object>) metrics.get("ai.request.stats");
        assertEquals(3, stats.get("count"));
        assertEquals(100L, stats.get("min"));
        assertEquals(300L, stats.get("p95"));
    }

    @Test
    void faultDetectorAggregatesFaultsAndRespectsAlertCooldown() {
        MonitoringService monitoringService = new MonitoringService();
        ReflectionTestUtils.setField(monitoringService, "retentionMs", 900_000L);

        AlertService alertService = new AlertService();
        InternalService internalService = mock(InternalService.class);
        ReflectionTestUtils.setField(alertService, "internalService", internalService);
        ReflectionTestUtils.setField(alertService, "cooldownMs", 300_000L);

        FaultDetector detector = new FaultDetector();
        ReflectionTestUtils.setField(detector, "monitoringService", monitoringService);
        ReflectionTestUtils.setField(detector, "alertService", alertService);
        ReflectionTestUtils.setField(detector, "detectionWindowMs", 300_000L);
        ReflectionTestUtils.setField(detector, "latencyP95ThresholdMs", 5_000L);
        ReflectionTestUtils.setField(detector, "errorRateThreshold", 0.1);
        ReflectionTestUtils.setField(detector, "minRequestCount", 10L);

        monitoringService.incrementCounter("ai.requests", 10);
        monitoringService.incrementCounter("ai.errors", 2);

        List<Map<String, Object>> firstDetection = detector.detectFaults();
        List<Map<String, Object>> secondDetection = detector.detectFaults();

        assertEquals(1, firstDetection.size());
        assertEquals(1, secondDetection.size());
        assertEquals("ERROR_RATE_HIGH", firstDetection.get(0).get("type"));
        assertFalse(detector.getAllFaults().isEmpty());
        verify(internalService, times(1)).sendAlert(anyMap());

        String faultId = String.valueOf(firstDetection.get(0).get("faultId"));
        Map<String, Object> resolveResult = detector.resolveFault(faultId);
        assertEquals("success", resolveResult.get("status"));
        assertTrue(detector.getAllFaults().stream()
                .anyMatch(fault -> "resolved".equals(fault.get("status"))));
    }
}
