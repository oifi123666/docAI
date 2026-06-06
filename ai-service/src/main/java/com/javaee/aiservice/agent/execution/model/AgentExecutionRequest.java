package com.javaee.aiservice.agent.execution.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Unified request for the product-level Agent execution chain.
 */
public class AgentExecutionRequest {

    private String task;
    private String conversationId;
    private String userId = "default";
    private String model;
    private Boolean ragEnabled = true;
    private Boolean dryRun = false;
    private Integer maxIterations = 3;
    private Integer maxToolCalls = 8;
    private String knowledgeBaseId = "default";
    private Boolean autoReplan = true;
    private Boolean reflectionEnabled;
    private Boolean returnIntermediateSteps = true;
    private Map<String, Object> context = new HashMap<>();
    private String continueTraceId;
    private String approvalToken;

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Boolean getRagEnabled() {
        return ragEnabled;
    }

    public void setRagEnabled(Boolean ragEnabled) {
        this.ragEnabled = ragEnabled;
    }

    public Boolean getDryRun() {
        return dryRun;
    }

    public void setDryRun(Boolean dryRun) {
        this.dryRun = dryRun;
    }

    public Integer getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(Integer maxIterations) {
        this.maxIterations = maxIterations;
    }

    public Integer getMaxToolCalls() {
        return maxToolCalls;
    }

    public void setMaxToolCalls(Integer maxToolCalls) {
        this.maxToolCalls = maxToolCalls;
    }

    public String getKnowledgeBaseId() {
        return knowledgeBaseId;
    }

    public void setKnowledgeBaseId(String knowledgeBaseId) {
        this.knowledgeBaseId = knowledgeBaseId;
    }

    public Boolean getAutoReplan() {
        return autoReplan;
    }

    public void setAutoReplan(Boolean autoReplan) {
        this.autoReplan = autoReplan;
    }

    public Boolean getReflectionEnabled() {
        return reflectionEnabled;
    }

    public void setReflectionEnabled(Boolean reflectionEnabled) {
        this.reflectionEnabled = reflectionEnabled;
    }

    public Boolean getReturnIntermediateSteps() {
        return returnIntermediateSteps;
    }

    public void setReturnIntermediateSteps(Boolean returnIntermediateSteps) {
        this.returnIntermediateSteps = returnIntermediateSteps;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context != null ? context : new HashMap<>();
    }

    public String getContinueTraceId() {
        return continueTraceId;
    }

    public void setContinueTraceId(String continueTraceId) {
        this.continueTraceId = continueTraceId;
    }

    public String getApprovalToken() {
        return approvalToken;
    }

    public void setApprovalToken(String approvalToken) {
        this.approvalToken = approvalToken;
    }
}
