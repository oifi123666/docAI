package com.javaee.aiservice.agent;

import com.javaee.aiservice.factory.AIServiceFactory;
import com.javaee.aiservice.model.ModelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 自定义Chat服务
 * 支持多模型选择
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    @Autowired
    private AIServiceFactory aiServiceFactory;

    /**
     * 调用默认模型（qwen-plus）
     * @param prompt 用户提示词
     * @return 响应内容
     */
    public String callChatApi(String prompt) {
        return callChatApiWithModelType(prompt, null);
    }

    /**
     * 调用指定模型
     * @param prompt 用户提示词
     * @param modelType 模型类型
     * @return 响应内容
     */
    public String callChatApiWithModelType(String prompt, ModelType modelType) {
        AIService aiService;
        if (modelType != null) {
            aiService = aiServiceFactory.getService(modelType);
        } else {
            aiService = aiServiceFactory.getDefaultService();
        }
        
        log.info("使用模型: {}", aiService.getModelType().getName());
        return aiService.callChat(prompt);
    }

    /**
     * 调用指定模型（通过模型代码）
     * @param prompt 用户提示词
     * @param modelCode 模型代码
     * @return 响应内容
     */
    public String callChatApiWithModelCode(String prompt, String modelCode) {
        if (modelCode == null || modelCode.isEmpty()) {
            return callChatApiWithModelType(prompt, null);
        }
        return callChatApiWithModelType(prompt, ModelType.fromCode(modelCode));
    }
}