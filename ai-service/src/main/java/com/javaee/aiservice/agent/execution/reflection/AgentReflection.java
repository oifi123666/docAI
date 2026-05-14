package com.javaee.aiservice.agent.execution.reflection;

import com.javaee.aiservice.agent.execution.model.AgentPlanStep;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured reflection emitted after each Agent execution round.
 */
public class AgentReflection {

    private int iteration;
    private boolean complete;
    private boolean continueExecution;
    private boolean requiresReplan;
    private boolean shouldAskUser;
    private int qualityScore = 100;
    private double confidence = 1.0d;
    private String reason;
    private List<String> issues = new ArrayList<>();
    private List<String> missingInfo = new ArrayList<>();
    private List<String> recommendedActions = new ArrayList<>();
    private List<AgentPlanStep> revisedPlan = new ArrayList<>();
    private String source = "model";
    private boolean fallback;
    private long createdAt = System.currentTimeMillis();

    public int getIteration() {
        return iteration;
    }

    public void setIteration(int iteration) {
        this.iteration = Math.max(0, iteration);
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public boolean isContinueExecution() {
        return continueExecution;
    }

    public void setContinueExecution(boolean continueExecution) {
        this.continueExecution = continueExecution;
    }

    public boolean isRequiresReplan() {
        return requiresReplan;
    }

    public void setRequiresReplan(boolean requiresReplan) {
        this.requiresReplan = requiresReplan;
    }

    public boolean isShouldAskUser() {
        return shouldAskUser;
    }

    public void setShouldAskUser(boolean shouldAskUser) {
        this.shouldAskUser = shouldAskUser;
    }

    public int getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(int qualityScore) {
        this.qualityScore = Math.max(0, Math.min(100, qualityScore));
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = Math.max(0.0d, Math.min(1.0d, confidence));
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<String> getIssues() {
        return issues;
    }

    public void setIssues(List<String> issues) {
        this.issues = issues != null ? issues : new ArrayList<>();
    }

    public List<String> getMissingInfo() {
        return missingInfo;
    }

    public void setMissingInfo(List<String> missingInfo) {
        this.missingInfo = missingInfo != null ? missingInfo : new ArrayList<>();
    }

    public List<String> getRecommendedActions() {
        return recommendedActions;
    }

    public void setRecommendedActions(List<String> recommendedActions) {
        this.recommendedActions = recommendedActions != null ? recommendedActions : new ArrayList<>();
    }

    public List<AgentPlanStep> getRevisedPlan() {
        return revisedPlan;
    }

    public void setRevisedPlan(List<AgentPlanStep> revisedPlan) {
        this.revisedPlan = revisedPlan != null ? revisedPlan : new ArrayList<>();
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isFallback() {
        return fallback;
    }

    public void setFallback(boolean fallback) {
        this.fallback = fallback;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
