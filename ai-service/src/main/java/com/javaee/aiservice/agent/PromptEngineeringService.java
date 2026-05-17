package com.javaee.aiservice.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 提示词工程服务
 * 使用Spring AI内置工具实现智能提示词拼接
 * 支持多种提示词模板的管理和动态生成
 */
@Service
public class PromptEngineeringService {

    private static final Logger log = LoggerFactory.getLogger(PromptEngineeringService.class);

    /**
     * 总结任务提示词模板
     */
    private static final String SUMMARIZE_TEMPLATE = """
            请对以下文本进行总结：

            {content}

            要求：
            1. 总结内容不超过{maxLength}字
            2. 保留核心要点
            3. 语言简洁明了
            """;

    /**
     * 关键词提取提示词模板
     */
    private static final String KEYWORD_EXTRACT_TEMPLATE = """
            请从以下文本中提取关键词：

            {content}

            要求：
            1. 提取{count}个核心关键词
            2. 用中文逗号分隔
            3. 按重要性排序
            """;

    /**
     * 智能问答提示词模板
     */
    private static final String QA_TEMPLATE = """
            你是一个智能助手，请基于以下信息回答用户问题：

            {knowledgeContext}

            {history}

            用户问题：{userInput}

            请提供准确、简洁的回答。
            """;

    private static final String RAG_ANSWER_TEMPLATE = """
            你是DocAI知识库问答助手。请严格基于【知识库片段】回答问题。
            如果片段中没有答案，请回答“知识库中未找到相关信息”，不要编造。

            【知识库片段】
            {knowledgeContext}

            【问题】
            {question}

            请给出结构化、可执行的答案，并在必要时引用来源编号。
            """;

    /**
     * 规划任务提示词模板
     */
    private static final String PLAN_TEMPLATE = """
            你是一个任务规划专家。请根据用户的任务描述，生成一个详细的执行计划。

            用户任务：{task}

            要求：
            1. 将任务分解为若干步骤
            2. 每个步骤用"- [步骤描述]"格式
            3. 预估每个步骤需要的技能
            4. 考虑可能的异常情况

            输出格式为JSON数组，包含以下字段：
            - step: 步骤编号
            - description: 步骤描述
            - skill: 需要的技能（如：file-delete, file-download；文件上传必须由用户通过上传页面或接口自行完成）
            - params: 预估参数（可选）
            """;

    /**
     * 执行总结提示词模板
     */
    private static final String EXECUTE_SUMMARY_TEMPLATE = """
            请总结以下任务执行结果：

            任务描述：{task}
            执行步骤：{steps}
            执行结果：{results}

            要求：
            1. 总结执行过程和结果
            2. 指出成功和失败的步骤
            3. 提供最终结论

            输出格式为JSON，包含以下字段：
            - success: 是否成功
            - summary: 执行总结
            - details: 详细结果
            """;

    /**
     * 反思优化提示词模板
     */
    private static final String REFLECT_TEMPLATE = """
            作为一个反思助手，请分析以下执行过程并提出优化建议：

            任务描述：{task}
            原始计划：{originalPlan}
            执行结果：{executionResult}

            请回答以下问题：
            1. 计划是否合理？
            2. 执行过程中有哪些问题？
            3. 如何优化下一次执行？

            输出格式为JSON，包含以下字段：
            - planQuality: 计划质量评价（优秀/良好/一般/较差）
            - issues: 问题列表
            - suggestions: 优化建议列表
            """;

    /**
     * 生成总结提示词
     * @param content 待总结内容
     * @param maxLength 最大长度
     * @return 提示词字符串
     */
    public String createSummarizePrompt(String content, int maxLength) {
        PromptTemplate template = new PromptTemplate(SUMMARIZE_TEMPLATE);
        Map<String, Object> params = new HashMap<>();
        params.put("content", content);
        params.put("maxLength", maxLength);
        return template.render(params);
    }

    /**
     * 生成关键词提取提示词
     * @param content 待分析内容
     * @param count 关键词数量
     * @return 提示词字符串
     */
    public String createKeywordExtractPrompt(String content, int count) {
        PromptTemplate template = new PromptTemplate(KEYWORD_EXTRACT_TEMPLATE);
        Map<String, Object> params = new HashMap<>();
        params.put("content", content);
        params.put("count", count);
        return template.render(params);
    }

    /**
     * 生成智能问答提示词
     * @param userInput 用户输入
     * @param knowledgeContext 知识库上下文
     * @param history 对话历史
     * @return 提示词字符串
     */
    public String createQAPrompt(String userInput, String knowledgeContext, List<String> history) {
        StringBuilder historyStr = new StringBuilder();
        if (history != null && !history.isEmpty()) {
            historyStr.append("对话历史：\n");
            for (String msg : history) {
                historyStr.append(msg).append("\n");
            }
            historyStr.append("\n");
        }

        PromptTemplate template = new PromptTemplate(QA_TEMPLATE);
        Map<String, Object> params = new HashMap<>();
        params.put("knowledgeContext", knowledgeContext != null ? knowledgeContext : "");
        params.put("history", historyStr.toString());
        params.put("userInput", userInput);

        return template.render(params);
    }

    public String createRagAnswerPrompt(String question, String knowledgeContext) {
        PromptTemplate template = new PromptTemplate(RAG_ANSWER_TEMPLATE);
        Map<String, Object> params = new HashMap<>();
        params.put("question", question);
        params.put("knowledgeContext", knowledgeContext != null ? knowledgeContext : "");
        return template.render(params);
    }

    /**
     * 生成规划任务提示词
     * @param task 任务描述
     * @return 提示词字符串
     */
    public String createPlanPrompt(String task) {
        PromptTemplate template = new PromptTemplate(PLAN_TEMPLATE);
        Map<String, Object> params = new HashMap<>();
        params.put("task", task);
        return template.render(params);
    }

    /**
     * 生成执行总结提示词
     * @param task 任务描述
     * @param steps 执行步骤
     * @param results 执行结果
     * @return 提示词字符串
     */
    public String createExecuteSummaryPrompt(String task, String steps, String results) {
        PromptTemplate template = new PromptTemplate(EXECUTE_SUMMARY_TEMPLATE);
        Map<String, Object> params = new HashMap<>();
        params.put("task", task);
        params.put("steps", steps);
        params.put("results", results);
        return template.render(params);
    }

    /**
     * 生成反思优化提示词
     * @param task 任务描述
     * @param originalPlan 原始计划
     * @param executionResult 执行结果
     * @return 提示词字符串
     */
    public String createReflectPrompt(String task, String originalPlan, String executionResult) {
        PromptTemplate template = new PromptTemplate(REFLECT_TEMPLATE);
        Map<String, Object> params = new HashMap<>();
        params.put("task", task);
        params.put("originalPlan", originalPlan);
        params.put("executionResult", executionResult);
        return template.render(params);
    }

    /**
     * 创建自定义提示词
     * @param templateStr 模板字符串
     * @param params 参数映射
     * @return 提示词字符串
     */
    public String createCustomPrompt(String templateStr, Map<String, Object> params) {
        PromptTemplate template = new PromptTemplate(templateStr);
        return template.render(params);
    }

    /**
     * 创建包含系统消息的提示词
     * @param systemContent 系统消息内容
     * @param userContent 用户消息内容
     * @return 提示词字符串
     */
    public String createChatPrompt(String systemContent, String userContent) {
        return systemContent + "\n\n" + userContent;
    }

    /**
     * 创建包含系统消息和参数的提示词
     * @param systemTemplate 系统消息模板
     * @param userTemplate 用户消息模板
     * @param params 参数映射
     * @return 提示词字符串
     */
    public String createChatPrompt(String systemTemplate, String userTemplate, Map<String, Object> params) {
        PromptTemplate sysTemplate = new PromptTemplate(systemTemplate);
        PromptTemplate userTmp = new PromptTemplate(userTemplate);

        return sysTemplate.render(params) + "\n\n" + userTmp.render(params);
    }
}
