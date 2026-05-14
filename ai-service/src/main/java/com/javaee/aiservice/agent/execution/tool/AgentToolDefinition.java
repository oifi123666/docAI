package com.javaee.aiservice.agent.execution.tool;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Metadata exposed to the planner so it can choose tools consistently.
 */
public class AgentToolDefinition {

    private final String name;
    private final String description;
    private final Map<String, String> parameters;
    private final Map<String, AgentToolParameterDefinition> parameterSchema;
    private final boolean destructive;
    private final String category;
    private final boolean requiresUserAction;
    private final String riskLevel;

    public AgentToolDefinition(String name, String description, Map<String, String> parameters, boolean destructive) {
        this(name, description, parameters, destructive, "general", false);
    }

    public AgentToolDefinition(String name, String description, Map<String, String> parameters,
                               boolean destructive, String category, boolean requiresUserAction) {
        this(name, description, parameters, toLegacySchema(parameters), destructive, category,
                requiresUserAction, destructive ? "high" : "low");
    }

    public AgentToolDefinition(String name, String description, Map<String, String> parameters,
                               Map<String, AgentToolParameterDefinition> parameterSchema,
                               boolean destructive, String category, boolean requiresUserAction, String riskLevel) {
        this.name = name;
        this.description = description;
        this.parameters = parameters != null ? parameters : new LinkedHashMap<>();
        this.parameterSchema = parameterSchema != null ? parameterSchema : new LinkedHashMap<>();
        this.destructive = destructive;
        this.category = category;
        this.requiresUserAction = requiresUserAction;
        this.riskLevel = riskLevel;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public Map<String, AgentToolParameterDefinition> getParameterSchema() {
        return parameterSchema;
    }

    public boolean isDestructive() {
        return destructive;
    }

    public String getCategory() {
        return category;
    }

    public boolean isRequiresUserAction() {
        return requiresUserAction;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    private static Map<String, AgentToolParameterDefinition> toLegacySchema(Map<String, String> parameters) {
        Map<String, AgentToolParameterDefinition> schema = new LinkedHashMap<>();
        if (parameters == null) {
            return schema;
        }
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            schema.put(entry.getKey(), new AgentToolParameterDefinition(
                    entry.getKey(), "string", entry.getValue(), false, null));
        }
        return schema;
    }
}
