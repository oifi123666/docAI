package com.javaee.aiservice.agent.execution.reflection;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaee.aiservice.agent.ChatService;
import com.javaee.aiservice.agent.execution.model.AgentExecutionRequest;
import com.javaee.aiservice.agent.execution.model.AgentPlanStep;
import com.javaee.aiservice.agent.execution.model.AgentToolResult;
import com.javaee.aiservice.agent.execution.tool.AgentToolDefinition;
import com.javaee.aiservice.agent.execution.tool.AgentToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reflects on each execution round and produces structured next-step decisions.
 */
@Service
public class AgentReflectionService {

    private static final Logger log = LoggerFactory.getLogger(AgentReflectionService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ChatService chatService;

    @Autowired
    private AgentToolRegistry toolRegistry;

    public AgentReflection reflect(AgentExecutionRequest request,
                                   List<AgentPlanStep> plan,
                                   List<AgentToolResult> results,
                                   Map<String, Object> context,
                                   int iteration,
                                   int maxIterations) {
        if (results == null || results.isEmpty()) {
            return heuristicReflection(iteration, maxIterations, null, "没有工具结果可供反思");
        }

        try {
            String raw = chatService.callChatApiWithModelCode(
                    buildReflectionPrompt(request, plan, results, context, iteration, maxIterations),
                    request.getModel());
            AgentReflection reflection = parseReflection(raw, iteration);
            normalizeDecision(reflection, request, results, iteration, maxIterations);
            return reflection;
        } catch (Exception e) {
            log.warn("Agent反思调用失败，使用启发式反思: {}", e.getMessage());
            return heuristicReflection(iteration, maxIterations, results.get(results.size() - 1), e.getMessage());
        }
    }

    private String buildReflectionPrompt(AgentExecutionRequest request,
                                         List<AgentPlanStep> plan,
                                         List<AgentToolResult> results,
                                         Map<String, Object> context,
                                         int iteration,
                                         int maxIterations) {
        return """
                你是DocAI Agent的反思器/审查器。请严格只输出一个JSON对象，不要输出解释、Markdown或代码块。
                你的任务是评估本轮执行是否足以完成用户任务，并决定是否需要补充规划。

                JSON格式:
                {
                  "complete": true/false,
                  "continueExecution": true/false,
                  "requiresReplan": true/false,
                  "shouldAskUser": true/false,
                  "qualityScore": 0-100,
                  "confidence": 0.0-1.0,
                  "reason": "简短原因",
                  "issues": ["问题1"],
                  "missingInfo": ["缺失信息1"],
                  "recommendedActions": ["建议动作1"],
                  "revisedPlan": [
                    {"id":"reflect-step-1","description":"...","toolName":"工具名","params":{},"reasoning":"...","successCriteria":"可选","retryPolicy":"none/exponential","maxRetries":1}
                  ]
                }

                revisedPlan步骤字段:
                - 必填: id, description, toolName, params, reasoning。
                - 可选: dependsOn, successCriteria, retryPolicy, maxRetries, riskLevel。
                - 后续步骤引用前序结果时，params 可使用 ${steps.<id>.data.<key>} 或 ${steps.<id>.observation}，并在 dependsOn 中填写对应 id。

                反思规则:
                - 工具结果已经足够回答用户任务时，complete=true，continueExecution=false，revisedPlan=[]。
                - 需要继续调用工具时，continueExecution=true，requiresReplan=true，并给出最多3个revisedPlan步骤。
                - 如果判断必须继续但不确定具体工具步骤，可以保持 continueExecution=true、requiresReplan=true、revisedPlan=[]，执行器会交给补充规划器。
                - 参数缺失或需要用户决定时，shouldAskUser=true，并优先使用ask-user工具。
                - 不要重复已经成功执行且参数相同的工具。
                - 只能使用工具列表中的toolName。
                - 如果已经达到最大轮次，continueExecution=false。

                当前轮次: %s / %s

                工具列表:
                %s

                用户任务:
                %s

                已执行计划:
                %s

                工具结果:
                %s

                上下文:
                %s
                """.formatted(iteration, maxIterations, toolCatalog(), request.getTask(), safeJson(plan),
                safeJson(results), safeJson(context));
    }

    private AgentReflection parseReflection(String raw, int iteration) throws Exception {
        Map<String, Object> map = objectMapper.readValue(stripObjectJson(raw), new TypeReference<>() {});
        AgentReflection reflection = new AgentReflection();
        reflection.setIteration(iteration);
        reflection.setComplete(booleanValue(map.get("complete"), false));
        reflection.setContinueExecution(booleanValue(map.get("continueExecution"), false));
        reflection.setRequiresReplan(booleanValue(map.get("requiresReplan"), false));
        reflection.setShouldAskUser(booleanValue(map.get("shouldAskUser"), false));
        reflection.setQualityScore(intValue(map.get("qualityScore"), reflection.getQualityScore()));
        reflection.setConfidence(doubleValue(map.get("confidence"), reflection.getConfidence()));
        reflection.setReason(asString(map.get("reason")));
        reflection.setIssues(stringList(map.get("issues")));
        reflection.setMissingInfo(stringList(map.get("missingInfo")));
        reflection.setRecommendedActions(stringList(map.get("recommendedActions")));
        reflection.setRevisedPlan(parseRevisedPlan(map.get("revisedPlan")));
        reflection.setSource("model");
        reflection.setFallback(false);
        return reflection;
    }

    private void normalizeDecision(AgentReflection reflection,
                                   AgentExecutionRequest request,
                                   List<AgentToolResult> results,
                                   int iteration,
                                   int maxIterations) {
        AgentToolResult last = results.get(results.size() - 1);
        if (iteration >= maxIterations) {
            reflection.setContinueExecution(false);
            reflection.setRequiresReplan(false);
            if (isBlank(reflection.getReason())) {
                reflection.setReason("已达到最大执行轮次");
            }
        }
        if (last.isRequiresAction()) {
            reflection.setComplete(false);
            reflection.setContinueExecution(false);
            reflection.setShouldAskUser(true);
        }
        if ("error".equals(last.getStatus()) && iteration < maxIterations && !last.isRequiresAction()) {
            reflection.setComplete(false);
            reflection.setContinueExecution(true);
            reflection.setRequiresReplan(true);
        }
        if (reflection.isComplete()) {
            reflection.setContinueExecution(false);
            reflection.setRequiresReplan(false);
            reflection.setRevisedPlan(List.of());
        }
        if (reflection.isShouldAskUser() && reflection.isContinueExecution() && reflection.getRevisedPlan().isEmpty()) {
            reflection.setRevisedPlan(List.of(buildAskUserStep(request, reflection)));
            reflection.setRequiresReplan(true);
        }
        reflection.setRevisedPlan(filterUsablePlan(reflection.getRevisedPlan()));
        if (reflection.isRequiresReplan() && reflection.isContinueExecution() && reflection.getRevisedPlan().isEmpty()) {
            reflection.getIssues().add("反思要求继续执行，但没有给出可用 revisedPlan");
        }
    }

    private AgentReflection heuristicReflection(int iteration, int maxIterations, AgentToolResult last, String reason) {
        AgentReflection reflection = new AgentReflection();
        reflection.setIteration(iteration);
        reflection.setSource("heuristic");
        reflection.setFallback(true);
        reflection.setReason(reason);

        if (last == null) {
            reflection.setComplete(false);
            reflection.setContinueExecution(false);
            reflection.setQualityScore(0);
            reflection.getIssues().add("没有可评估的工具结果");
            return reflection;
        }

        if (last.isRequiresAction()) {
            reflection.setComplete(false);
            reflection.setContinueExecution(false);
            reflection.setShouldAskUser(true);
            reflection.setQualityScore(40);
            reflection.getMissingInfo().add(last.getMessage());
            return reflection;
        }

        if ("error".equals(last.getStatus())) {
            reflection.setComplete(false);
            reflection.setContinueExecution(iteration < maxIterations);
            reflection.setRequiresReplan(iteration < maxIterations);
            reflection.setQualityScore(20);
            reflection.getIssues().add(last.getMessage());
            reflection.getRecommendedActions().add("修正失败步骤参数或选择替代工具后重新规划");
            return reflection;
        }

        boolean hasFinalSignal = last.getData().containsKey("answer")
                || last.getData().containsKey("fileUrl")
                || last.getData().containsKey("result")
                || !"rag-search".equals(last.getToolName());
        reflection.setComplete(hasFinalSignal);
        reflection.setContinueExecution(!hasFinalSignal && iteration < maxIterations);
        reflection.setRequiresReplan(reflection.isContinueExecution());
        reflection.setQualityScore(hasFinalSignal ? 80 : 55);
        reflection.setReason(hasFinalSignal ? "最后一次工具结果可以支持最终回答" : "需要继续补充信息");
        return reflection;
    }

    private List<AgentPlanStep> parseRevisedPlan(Object value) {
        if (!(value instanceof List<?> list)) {
            return new ArrayList<>();
        }
        List<AgentPlanStep> steps = new ArrayList<>();
        int index = 1;
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            AgentPlanStep step = new AgentPlanStep();
            step.setId(firstNonBlank(asString(map.get("id")), "reflect-step-" + index));
            step.setDescription(asString(map.get("description")));
            step.setToolName(firstNonBlank(asString(map.get("toolName")), asString(map.get("tool")), "direct-answer"));
            step.setReasoning(asString(map.get("reasoning")));
            step.setThought(asString(map.get("thought")));
            step.setSuccessCriteria(asString(map.get("successCriteria")));
            step.setRetryPolicy(firstNonBlank(asString(map.get("retryPolicy")), "exponential"));
            step.setMaxRetries(intValue(map.get("maxRetries"), 1));
            Object dependsOn = map.get("dependsOn");
            if (dependsOn instanceof List<?> dependsOnList) {
                List<String> ids = new ArrayList<>();
                for (Object dependency : dependsOnList) {
                    if (dependency != null && !isBlank(dependency.toString())) {
                        ids.add(dependency.toString());
                    }
                }
                step.setDependsOn(ids);
            }
            String riskLevel = asString(map.get("riskLevel"));
            if (!isBlank(riskLevel)) {
                step.setRiskLevel(riskLevel);
            }
            Object params = map.get("params");
            if (params instanceof Map<?, ?> paramsMap) {
                step.setParams(toStringObjectMap(paramsMap));
            }
            steps.add(step);
            index++;
        }
        return steps;
    }

    private List<AgentPlanStep> filterUsablePlan(List<AgentPlanStep> steps) {
        List<AgentPlanStep> usable = new ArrayList<>();
        for (AgentPlanStep step : steps) {
            if (step == null) {
                continue;
            }
            if (!toolRegistry.contains(step.getToolName())) {
                step.setToolName("direct-answer");
            }
            usable.add(step);
        }
        return usable;
    }

    private AgentPlanStep buildAskUserStep(AgentExecutionRequest request, AgentReflection reflection) {
        Map<String, Object> params = new HashMap<>();
        String missing = String.join("、", reflection.getMissingInfo());
        params.put("question", isBlank(missing)
                ? "请补充完成任务所需的信息：" + request.getTask()
                : "请补充以下信息：" + missing);
        params.put("missingFields", missing);
        AgentPlanStep step = new AgentPlanStep("reflect-ask-user", "向用户澄清缺失信息", "ask-user", params);
        step.setReasoning("反思阶段发现信息不足，需要用户补充");
        step.setRetryPolicy("none");
        return step;
    }

    private String toolCatalog() {
        StringBuilder builder = new StringBuilder();
        for (AgentToolDefinition tool : toolRegistry.list()) {
            builder.append("- ").append(tool.getName())
                    .append(": ").append(tool.getDescription())
                    .append(" 参数: ").append(safeJson(tool.getParameterSchema()))
                    .append("\n");
        }
        return builder.toString();
    }

    private String stripObjectJson(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```[a-zA-Z]*\\s*", "");
            text = text.replaceFirst("\\s*```$", "");
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        return start >= 0 && end > start ? text.substring(start, end + 1) : text;
    }

    private Map<String, Object> toStringObjectMap(Map<?, ?> map) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null) {
                result.put(entry.getKey().toString(), entry.getValue());
            }
        }
        return result;
    }

    private List<String> stringList(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (item != null && !isBlank(item.toString())) {
                    result.add(item.toString());
                }
            }
        } else if (value != null && !isBlank(value.toString())) {
            result.add(value.toString());
        }
        return result;
    }

    private String safeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private boolean booleanValue(Object value, boolean defaultValue) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value != null ? Boolean.parseBoolean(value.toString()) : defaultValue;
    }

    private int intValue(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value != null ? Integer.parseInt(value.toString()) : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private double doubleValue(Object value, double defaultValue) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return value != null ? Double.parseDouble(value.toString()) : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
