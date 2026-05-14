package com.javaee.aiservice.aiops;

import com.javaee.aiservice.aiops.alert.AlertService;
import com.javaee.aiservice.aiops.model.FaultRecord;
import com.javaee.aiservice.aiops.model.TimerStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 故障检测器
 * 自动检测系统故障和异常
 * 支持自动故障处理和告警
 */
@Component
public class FaultDetector {

    private static final Logger log = LoggerFactory.getLogger(FaultDetector.class);

    @Autowired
    private MonitoringService monitoringService;

    @Autowired
    private AlertService alertService;

    @Value("${ai.aiops.detection.window-ms:300000}")
    private long detectionWindowMs;

    @Value("${ai.aiops.detection.latency-p95-threshold-ms:5000}")
    private long latencyP95ThresholdMs;

    @Value("${ai.aiops.detection.error-rate-threshold:0.1}")
    private double errorRateThreshold;

    @Value("${ai.aiops.detection.min-request-count:10}")
    private long minRequestCount;

    private final ConcurrentHashMap<String, FaultRecord> faultsById = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, FaultRecord> activeFaultsByFingerprint = new ConcurrentHashMap<>();

    public List<Map<String, Object>> detectFaults() {
        log.info("开始故障检测");

        List<FaultRecord> detected = new ArrayList<>();

        detectPerformanceIssues(detected);
        detectThresholdBreaches(detected);

        log.info("故障检测完成，发现{}个活跃故障", detected.size());
        return detected.stream()
                .map(FaultRecord::toMap)
                .toList();
    }

    private void detectPerformanceIssues(List<FaultRecord> issues) {
        String fingerprint = "PERFORMANCE_DEGRADED:ai.request";
        TimerStats stats = monitoringService.getTimerStatsObject("ai.request", detectionWindowMs);

        if (stats.getCount() >= minRequestCount && stats.getP95() > latencyP95ThresholdMs) {
            FaultRecord fault = createOrUpdateFault(
                    "PERFORMANCE_DEGRADED",
                    fingerprint,
                    "AI请求 P95 响应时间超过阈值: p95=" + stats.getP95()
                            + "ms, threshold=" + latencyP95ThresholdMs + "ms, windowMs=" + detectionWindowMs
            );
            issues.add(fault);
            return;
        }

        autoResolve(fingerprint);
    }

    private void detectThresholdBreaches(List<FaultRecord> issues) {
        String fingerprint = "ERROR_RATE_HIGH:ai.request";
        long errorCount = monitoringService.getCounter("ai.errors", detectionWindowMs);
        long totalCount = monitoringService.getCounter("ai.requests", detectionWindowMs);
        double errorRate = totalCount > 0 ? (double) errorCount / totalCount : 0.0;

        if (totalCount >= minRequestCount && errorRate > errorRateThreshold) {
            FaultRecord fault = createOrUpdateFault(
                    "ERROR_RATE_HIGH",
                    fingerprint,
                    "AI请求错误率超过阈值: errorRate=" + errorRate
                            + ", errors=" + errorCount
                            + ", total=" + totalCount
                            + ", threshold=" + errorRateThreshold
            );
            issues.add(fault);
            return;
        }

        autoResolve(fingerprint);
    }

    private FaultRecord createOrUpdateFault(String type, String fingerprint, String message) {
        long now = System.currentTimeMillis();
        FaultRecord record = activeFaultsByFingerprint.compute(fingerprint, (key, existing) -> {
            if (existing == null) {
                String faultId = type + ":" + UUID.randomUUID();
                FaultRecord created = new FaultRecord(faultId, type, fingerprint, message, now);
                faultsById.put(faultId, created);
                return created;
            }
            existing.occur(message, now);
            return existing;
        });

        alertService.sendIfAllowed(record);
        return record;
    }

    public Map<String, Object> resolveFault(String faultId) {
        log.info("处理故障: faultId={}", faultId);

        FaultRecord record = faultsById.get(faultId);
        if (record == null) {
            return Map.of(
                    "status", "error",
                    "message", "故障不存在"
            );
        }

        record.resolve(System.currentTimeMillis());
        activeFaultsByFingerprint.remove(record.getFingerprint(), record);
        alertService.clearCooldown(record.getFingerprint());

        return Map.of(
                "status", "success",
                "faultId", faultId,
                "type", record.getType(),
                "message", "故障已处理"
        );
    }

    public List<Map<String, Object>> getAllFaults() {
        return faultsById.values().stream()
                .sorted(Comparator.comparingLong(FaultRecord::getLastOccurrence).reversed())
                .map(FaultRecord::toMap)
                .toList();
    }

    public void resetFaults() {
        faultsById.clear();
        activeFaultsByFingerprint.clear();
        alertService.reset();
        log.info("AIOps 故障记录已重置");
    }

    private void autoResolve(String fingerprint) {
        FaultRecord record = activeFaultsByFingerprint.remove(fingerprint);
        if (record != null) {
            record.resolve(System.currentTimeMillis());
            alertService.clearCooldown(fingerprint);
            log.info("故障已自动恢复: faultId={}, fingerprint={}", record.getFaultId(), fingerprint);
        }
    }

    @Scheduled(fixedDelayString = "${ai.aiops.detection.interval-ms:60000}")
    public void scheduledDetection() {
        detectFaults();
    }
}
