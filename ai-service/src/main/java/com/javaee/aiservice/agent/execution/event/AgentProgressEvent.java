package com.javaee.aiservice.agent.execution.event;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工作台实时进度事件，统一承载 Agent 执行与知识索引任务状态。
 */
public class AgentProgressEvent {

    private String eventType;
    private String traceId;
    private String jobId;
    private String userId;
    private String status;
    private String message;
    private int progress;
    private long timestamp = System.currentTimeMillis();
    private Map<String, Object> payload = new LinkedHashMap<>();

    public static AgentProgressEvent of(String eventType, String userId, String status, String message) {
        AgentProgressEvent event = new AgentProgressEvent();
        event.setEventType(eventType);
        event.setUserId(userId);
        event.setStatus(status);
        event.setMessage(message);
        return event;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = Math.max(0, Math.min(100, progress));
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload != null ? payload : new LinkedHashMap<>();
    }
}
