package com.javaee.aiservice;

import com.javaee.aiservice.agent.ChatService;
import com.javaee.aiservice.factory.AIServiceFactory;
import com.javaee.aiservice.model.ModelType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class AllModelsTest {

    private static final Logger log = LoggerFactory.getLogger(AllModelsTest.class);

    @Autowired
    private AIServiceFactory aiServiceFactory;

    @Autowired
    private ChatService chatService;

    private static final String TEST_PROMPT = "请用100字以内介绍一下你自己";

    @Test
    public void testAllModels() {
        log.info("========================================");
        log.info("开始测试所有可用模型");
        log.info("========================================");

        Map<ModelType, String> results = new HashMap<>();
        List<ModelType> failedModels = new ArrayList<>();

        // 获取所有可用模型
        Map<ModelType, com.javaee.aiservice.agent.AIService> services = aiServiceFactory.getAllServices();
        
        for (Map.Entry<ModelType, com.javaee.aiservice.agent.AIService> entry : services.entrySet()) {
            ModelType modelType = entry.getKey();
            com.javaee.aiservice.agent.AIService service = entry.getValue();
            
            if (!service.isAvailable()) {
                log.warn("模型 {} 已禁用，跳过测试", modelType.getName());
                continue;
            }
            
            log.info("----------------------------------------");
            log.info("测试模型: {} ({})", modelType.getName(), modelType.getCode());
            
            try {
                String response = chatService.callChatApiWithModelCode(TEST_PROMPT, modelType.getCode());
                
                log.info("✓ 模型 {} 调用成功!", modelType.getName());
                log.info("响应: {}", response);
                
                results.put(modelType, response);
                
            } catch (Exception e) {
                log.error("✗ 模型 {} 调用失败!", modelType.getName(), e);
                failedModels.add(modelType);
            }
        }

        log.info("========================================");
        log.info("测试总结");
        log.info("========================================");
        log.info("成功模型: {}/{}", results.size(), services.size());
        
        if (!results.isEmpty()) {
            log.info("成功的模型:");
            results.keySet().forEach(model -> log.info("  - {} ({})", model.getName(), model.getCode()));
        }
        
        if (!failedModels.isEmpty()) {
            log.warn("失败的模型:");
            failedModels.forEach(model -> log.warn("  - {} ({})", model.getName(), model.getCode()));
        }
        
        log.info("========================================");
    }
}
