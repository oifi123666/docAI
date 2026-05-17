package com.javaee.aiservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "ai.models")
public class MultiModelConfig {
    
    private Map<String, ModelConfig> dashscope = new HashMap<>();
    private Map<String, ModelConfig> openai = new HashMap<>();
    
    public Map<String, ModelConfig> getDashscope() {
        return dashscope;
    }
    
    public void setDashscope(Map<String, ModelConfig> dashscope) {
        this.dashscope = dashscope;
    }
    
    public Map<String, ModelConfig> getOpenai() {
        return openai;
    }
    
    public void setOpenai(Map<String, ModelConfig> openai) {
        this.openai = openai;
    }
    
    public static class ModelConfig {
        private String apiKey;
        private String model;
        private String baseUrl;
        private boolean enabled = true;
        
        public String getApiKey() {
            return apiKey;
        }
        
        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
        
        public String getModel() {
            return model;
        }
        
        public void setModel(String model) {
            this.model = model;
        }
        
        public String getBaseUrl() {
            return baseUrl;
        }
        
        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
