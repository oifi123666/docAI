package com.javaee.aiservice.controller;

import com.javaee.aiservice.async.AsyncAIJobService;
import com.javaee.aiservice.async.AsyncAIJobVO;
import com.javaee.aiservice.agent.execution.model.AgentExecutionRequest;
import com.javaee.aiservice.dto.AsyncChatDTO;
import com.javaee.aiservice.dto.KeywordExtractDTO;
import com.javaee.aiservice.dto.TextAnalyzeDTO;
import com.javaee.aiservice.dto.TextSummarizeDTO;
import com.javaee.aiservice.security.RequestUserContext;
import com.javaee.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Asynchronous AI endpoints backed by RabbitMQ.
 */
@RestController
@RequestMapping("/api/ai/async")
@Tag(name = "异步AI处理", description = "通过RabbitMQ异步执行AI模型请求")
public class AsyncAIController {

    @Autowired
    private AsyncAIJobService asyncAIJobService;

    @Autowired
    private RequestUserContext requestUserContext;

    @PostMapping("/summarize")
    @Operation(summary = "异步文档摘要", description = "提交摘要任务到RabbitMQ，立即返回jobId")
    public Result<AsyncAIJobVO> submitSummarize(
            @RequestBody TextSummarizeDTO dto,
            @Parameter(description = "选择AI模型（可选，默认qwen3.6-plus）",
                    in = ParameterIn.QUERY,
                    schema = @Schema(allowableValues = {"qwen3.6-plus", "glm-5", "kimi-k2.5", "MiniMax-M2.5"},
                            defaultValue = "qwen3.6-plus"))
            @RequestParam(required = false) String model) {
        return Result.success(asyncAIJobService.submit("summarize", dto, model, requestUserContext.getRequiredUserId()));
    }

    @PostMapping("/keywords")
    @Operation(summary = "异步关键词提取", description = "提交关键词提取任务到RabbitMQ，立即返回jobId")
    public Result<AsyncAIJobVO> submitKeywords(
            @RequestBody KeywordExtractDTO dto,
            @Parameter(description = "选择AI模型（可选，默认qwen3.6-plus）",
                    in = ParameterIn.QUERY,
                    schema = @Schema(allowableValues = {"qwen3.6-plus", "glm-5", "kimi-k2.5", "MiniMax-M2.5"},
                            defaultValue = "qwen3.6-plus"))
            @RequestParam(required = false) String model) {
        return Result.success(asyncAIJobService.submit("keywords", dto, model, requestUserContext.getRequiredUserId()));
    }

    @PostMapping("/analyze")
    @Operation(summary = "异步文档分析", description = "提交文档分析任务到RabbitMQ，立即返回jobId")
    public Result<AsyncAIJobVO> submitAnalyze(@RequestBody TextAnalyzeDTO dto) {
        return Result.success(asyncAIJobService.submit("analyze", dto, null, requestUserContext.getRequiredUserId()));
    }

    @PostMapping("/chat")
    @Operation(summary = "异步通用模型调用", description = "提交通用prompt到RabbitMQ，后台调用模型并写回结果")
    public Result<AsyncAIJobVO> submitChat(
            @RequestBody AsyncChatDTO dto,
            @Parameter(description = "选择AI模型（可选，默认qwen3.6-plus）",
                    in = ParameterIn.QUERY,
                    schema = @Schema(allowableValues = {"qwen3.6-plus", "glm-5", "kimi-k2.5", "MiniMax-M2.5"},
                            defaultValue = "qwen3.6-plus"))
            @RequestParam(required = false) String model) {
        return Result.success(asyncAIJobService.submit("chat", dto, model, requestUserContext.getRequiredUserId()));
    }

    @PostMapping("/agent/execute")
    @Operation(summary = "异步Agent任务", description = "提交统一Agent任务到RabbitMQ，立即返回jobId")
    public Result<AsyncAIJobVO> submitAgent(@RequestBody AgentExecutionRequest request) {
        String userId = requestUserContext.getRequiredUserId();
        request.setUserId(userId);
        return Result.success(asyncAIJobService.submit("agent", request, request.getModel(), userId));
    }

    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "查询异步任务", description = "根据jobId查询任务状态和执行结果")
    public Result<AsyncAIJobVO> getJob(
            @Parameter(description = "异步任务ID") @PathVariable String jobId) {
        AsyncAIJobVO job = asyncAIJobService.getJob(
                jobId,
                requestUserContext.getRequiredUserId(),
                requestUserContext.isAdmin()
        );
        return Result.success(job);
    }
}
