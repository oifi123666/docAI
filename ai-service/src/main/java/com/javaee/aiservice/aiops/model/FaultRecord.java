package com.javaee.aiservice.aiops.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 聚合后的故障记录，同一 fingerprint 在未恢复前只维护一条记录。
 */
public class FaultRecord {

    private final String faultId;
    private final String type;
    private final String fingerprint;
    private final long firstOccurrence;
    private long lastOccurrence;
    private int count;
    private String message;
    private String status;
    private long resolvedAt;

    public FaultRecord(String faultId, String type, String fingerprint, String message, long timestamp) {
        this.faultId = faultId;
        this.type = type;
        this.fingerprint = fingerprint;
        this.message = message;
        this.firstOccurrence = timestamp;
        this.lastOccurrence = timestamp;
        this.count = 1;
        this.status = "detected";
    }

    public void occur(String message, long timestamp) {
        this.message = message;
        this.lastOccurrence = timestamp;
        this.count++;
        if ("resolved".equals(status)) {
            this.status = "detected";
            this.resolvedAt = 0L;
        }
    }

    public void resolve(long timestamp) {
        this.status = "resolved";
        this.resolvedAt = timestamp;
        this.lastOccurrence = timestamp;
    }

    public String getFaultId() {
        return faultId;
    }

    public String getType() {
        return type;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public String getMessage() {
        return message;
    }

    public String getStatus() {
        return status;
    }

    public long getLastOccurrence() {
        return lastOccurrence;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("faultId", faultId);
        result.put("type", type);
        result.put("fingerprint", fingerprint);
        result.put("message", message);
        result.put("firstOccurrence", firstOccurrence);
        result.put("lastOccurrence", lastOccurrence);
        result.put("count", count);
        result.put("status", status);
        if (resolvedAt > 0) {
            result.put("resolvedAt", resolvedAt);
        }
        return result;
    }
}
