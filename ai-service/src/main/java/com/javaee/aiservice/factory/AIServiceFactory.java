package com.javaee.aiservice.factory;

import com.javaee.aiservice.agent.AIService;
import com.javaee.aiservice.agent.DashScopeAIService;
import com.javaee.aiservice.agent.OpenAIAIService;
import com.javaee.aiservice.config.MultiModelConfig;
import com.javaee.aiservice.model.ModelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AIServiceFactory {
    
    private static final Logger log = LoggerFactory.getLogger(AIServiceFactory.class);
    
    @Autowired
    private MultiModelConfig multiModelConfig;
    
    @Value("${spring.ai.dashscope.api-key}")
    private String defaultApiKey;
    
    @Value("${spring.ai.openai.api-key:#{null}}")
    private String defaultOpenAiApiKey;
    
    @Value("${spring.ai.openai.base-url:#{null}}")
    private String defaultOpenAiBaseUrl;
    
    private final Map<ModelType, AIService> serviceMap = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        log.info("初始化AI服务工厂...");
        
        for (ModelType modelType : ModelType.values()) {
            String provider = modelType.getProvider();
            
            if ("dashscope".equals(provider)) {
                String apiKey = defaultApiKey;
                boolean enabled = true;
                
                if (multiModelConfig.getDashscope().containsKey(modelType.getCode())) {
                    MultiModelConfig.ModelConfig config = multiModelConfig.getDashscope().get(modelType.getCode());
                    if (config.getApiKey() != null) {
                        apiKey = config.getApiKey();
                    }
                    enabled = config.isEnabled();
                }
                
                DashScopeAIService service = new DashScopeAIService(apiKey, modelType, enabled);
                serviceMap.put(modelType, service);
                log.info("注册DashScope模型: {} (enabled: {})", modelType.getName(), enabled);
            } else if ("openai".equals(provider)) {
                String apiKey = defaultOpenAiApiKey;
                String baseUrl = defaultOpenAiBaseUrl;
                boolean enabled = true;
                
                if (multiModelConfig.getOpenai().containsKey(modelType.getCode())) {
                    MultiModelConfig.ModelConfig config = multiModelConfig.getOpenai().get(modelType.getCode());
                    if (config.getApiKey() != null) {
                        apiKey = config.getApiKey();
                    }
                    if (config.getBaseUrl() != null) {
                        baseUrl = config.getBaseUrl();
                    }
                    enabled = config.isEnabled();
                }
                
                if (apiKey == null || baseUrl == null) {
                    log.warn("模型 {} 缺少apiKey或baseUrl配置，跳过注册", modelType.getName());
                    continue;
                }
                
                OpenAIAIService service = new OpenAIAIService(apiKey, baseUrl, modelType, enabled);
                serviceMap.put(modelType, service);
                log.info("注册OpenAI兼容模型: {} (enabled: {})", modelType.getName(), enabled);
            }
        }
        
        log.info("AI服务工厂初始化完成，共注册 {} 个模型", serviceMap.size());
    }
    
    public AIService getService(ModelType modelType) {
        AIService service = serviceMap.get(modelType);
        if (service == null) {
            throw new IllegalArgumentException("不支持的模型类型: " + modelType);
        }
        if (!service.isAvailable()) {
            throw new IllegalStateException("模型 " + modelType.getName() + " 已禁用");
        }
        return service;
    }
    
    public AIService getService(String modelCode) {
        return getService(ModelType.fromCode(modelCode));
    }
    
    public AIService getDefaultService() {
        return getService(ModelType.QWEN36_PLUS);
    }
    
    public Map<ModelType, AIService> getAllServices() {
        return new HashMap<>(serviceMap);
    }
}
