package com.javaee.aiservice.aiops.alert;

import com.javaee.aiservice.aiops.model.FaultRecord;
import com.javaee.aiservice.internal.InternalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    @Autowired
    private InternalService internalService;

    @Value("${ai.aiops.alert.cooldown-ms:300000}")
    private long cooldownMs;

    private final ConcurrentHashMap<String, Long> lastSentAt = new ConcurrentHashMap<>();

    public boolean sendIfAllowed(FaultRecord record) {
        long now = System.currentTimeMillis();
        Long previous = lastSentAt.get(record.getFingerprint());
        if (previous != null && now - previous < cooldownMs) {
            log.debug("告警处于冷却期，跳过发送: fingerprint={}", record.getFingerprint());
            return false;
        }

        lastSentAt.put(record.getFingerprint(), now);
        Map<String, Object> alertParams = new HashMap<>();
        alertParams.put("faultId", record.getFaultId());
        alertParams.put("type", record.getType());
        alertParams.put("fingerprint", record.getFingerprint());
        alertParams.put("message", record.getMessage());
        alertParams.put("status", record.getStatus());
        alertParams.put("timestamp", now);

        log.warn("发送 AIOps 告警: {}", alertParams);
        internalService.sendAlert(alertParams);
        return true;
    }

    public void clearCooldown(String fingerprint) {
        lastSentAt.remove(fingerprint);
    }

    public void reset() {
        lastSentAt.clear();
    }
}
