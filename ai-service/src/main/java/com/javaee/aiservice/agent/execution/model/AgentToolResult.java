package com.javaee.aiservice.agent.execution.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Normalized result returned by every Agent tool.
 */
public class AgentToolResult {

    private String toolName;
    private String status;
    private String message;
    private boolean requiresAction;
    private String stepId;
    private long startedAt;
    private long finishedAt;
    private long durationMs;
    private Map<String, Object> data = new HashMap<>();

    public static AgentToolResult success(String toolName, String message, Map<String, Object> data) {
        AgentToolResult result = new AgentToolResult();
        result.setToolName(toolName);
        result.setStatus("success");
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    public static AgentToolResult actionRequired(String toolName, String message, Map<String, Object> data) {
        AgentToolResult result = success(toolName, message, data);
        result.setStatus("action_required");
        result.setRequiresAction(true);
        return result;
    }

    public static AgentToolResult error(String toolName, String message) {
        AgentToolResult result = new AgentToolResult();
        result.setToolName(toolName);
        result.setStatus("error");
        result.setMessage(message);
        return result;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
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

    public boolean isRequiresAction() {
        return requiresAction;
    }

    public void setRequiresAction(boolean requiresAction) {
        this.requiresAction = requiresAction;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data != null ? data : new HashMap<>();
    }

    public String getStepId() {
        return stepId;
    }

    public void setStepId(String stepId) {
        this.stepId = stepId;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }

    public long getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(long finishedAt) {
        this.finishedAt = finishedAt;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }
}
