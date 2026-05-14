package com.javaee.aiservice.agent.execution.model;

/**
 * Plan 步骤状态机：标准化状态字符串，避免散落的 magic value。
 */
public enum AgentStepStatus {
    PENDING("pending"),
    PLANNED("planned"),
    RUNNING("running"),
    SUCCESS("success"),
    ERROR("error"),
    SKIPPED("skipped"),
    BLOCKED("blocked"),
    WAITING_USER("waiting_user"),
    CANCELLED("cancelled");

    private final String value;

    AgentStepStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static boolean isTerminal(String status) {
        return SUCCESS.value.equals(status)
                || ERROR.value.equals(status)
                || SKIPPED.value.equals(status)
                || BLOCKED.value.equals(status)
                || CANCELLED.value.equals(status);
    }

    public static boolean isSuccess(String status) {
        return SUCCESS.value.equals(status);
    }
}
