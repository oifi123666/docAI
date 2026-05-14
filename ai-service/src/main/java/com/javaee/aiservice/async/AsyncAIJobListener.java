package com.javaee.aiservice.async;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaee.aiservice.agent.ChatService;
import com.javaee.aiservice.config.AiRabbitMQConfig;
import com.javaee.aiservice.dto.AsyncChatDTO;
import com.javaee.aiservice.dto.KeywordExtractDTO;
import com.javaee.aiservice.dto.TextAnalyzeDTO;
import com.javaee.aiservice.dto.TextSummarizeDTO;
import com.javaee.aiservice.service.AIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Consumes queued model jobs and writes results back to Redis.
 */
@Component
public class AsyncAIJobListener {

    private static final Logger log = LoggerFactory.getLogger(AsyncAIJobListener.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AsyncAIJobService asyncAIJobService;

    @Autowired
    private AIService aiService;

    @Autowired
    private ChatService chatService;

    @RabbitListener(queues = AiRabbitMQConfig.AI_MODEL_REQUEST_QUEUE)
    public void handleModelJob(AsyncAIJobMessage message) {
        log.info("收到异步AI任务: jobId={}, type={}, model={}",
                message.getJobId(), message.getType(), message.getModel());

        asyncAIJobService.markRunning(message.getJobId());
        try {
            Object result = execute(message);
            asyncAIJobService.markSuccess(message.getJobId(), result);
            log.info("异步AI任务执行成功: jobId={}", message.getJobId());
        } catch (Exception e) {
            log.error("异步AI任务执行失败: jobId={}", message.getJobId(), e);
            asyncAIJobService.markFailed(message.getJobId(), e.getMessage());
        }
    }

    private Object execute(AsyncAIJobMessage message) {
        String type = message.getType();
        return switch (type) {
            case "summarize" -> aiService.summarize(
                    objectMapper.convertValue(message.getPayload(), TextSummarizeDTO.class),
                    message.getModel()
            );
            case "keywords" -> aiService.extractKeywords(
                    objectMapper.convertValue(message.getPayload(), KeywordExtractDTO.class),
                    message.getModel()
            );
            case "analyze" -> aiService.analyze(
                    objectMapper.convertValue(message.getPayload(), TextAnalyzeDTO.class)
            );
            case "chat" -> executeChat(message);
            default -> throw new IllegalArgumentException("不支持的异步AI任务类型: " + type);
        };
    }

    private String executeChat(AsyncAIJobMessage message) {
        AsyncChatDTO dto = objectMapper.convertValue(message.getPayload(), AsyncChatDTO.class);
        if (dto.getPrompt() == null || dto.getPrompt().isBlank()) {
            throw new IllegalArgumentException("prompt不能为空");
        }
        return chatService.callChatApiWithModelCode(dto.getPrompt(), message.getModel());
    }
}
