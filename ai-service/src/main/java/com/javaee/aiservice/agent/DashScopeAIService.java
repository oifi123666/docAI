package com.javaee.aiservice.agent;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.javaee.aiservice.model.ModelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class DashScopeAIService implements AIService {
    
    private static final Logger log = LoggerFactory.getLogger(DashScopeAIService.class);
    
    private final String apiKey;
    private final ModelType modelType;
    private final boolean enabled;
    
    public DashScopeAIService(String apiKey, ModelType modelType, boolean enabled) {
        this.apiKey = apiKey;
        this.modelType = modelType;
        this.enabled = enabled;
    }
    
    @Override
    public String callChat(String prompt) {
        if (!enabled) {
            throw new RuntimeException("Model " + modelType.getName() + " is disabled");
        }
        
        log.info("调用DashScope模型: {}, prompt长度: {}", modelType.getName(), prompt.length());
        
        try {
            Generation gen = new Generation();
            
            Message userMsg = Message.builder()
                    .role(Role.USER.getValue())
                    .content(prompt)
                    .build();

            GenerationParam param;
            
            // 构建模型调用参数
            param = GenerationParam.builder()
                    .apiKey(apiKey)
                    .model(modelType.getCode())
                    .resultFormat("message")
                    .messages(Arrays.asList(userMsg))
                    .build();

            GenerationResult result = gen.call(param);
            
            if (result != null && result.getOutput() != null 
                    && result.getOutput().getChoices() != null 
                    && !result.getOutput().getChoices().isEmpty()) {
                
                String content = result.getOutput().getChoices().get(0).getMessage().getContent();
                String reasoning = result.getOutput().getChoices().get(0).getMessage().getReasoningContent();
                
                log.info("{}响应成功", modelType.getName());
                if (reasoning != null && !reasoning.isEmpty()) {
                    log.debug("思考过程: {}", reasoning);
                }
                
                return content != null ? content : "";
            }
            
            throw new RuntimeException("API返回结果为空");

        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            log.error("调用{}失败", modelType.getName(), e);
            throw new RuntimeException("调用" + modelType.getName() + "失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public ModelType getModelType() {
        return modelType;
    }
    
    @Override
    public boolean isAvailable() {
        return enabled;
    }
}
