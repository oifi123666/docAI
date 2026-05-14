package com.javaee.aiservice.agent;

import com.javaee.aiservice.model.ModelType;

public interface AIService {
    
    String callChat(String prompt);
    
    ModelType getModelType();
    
    boolean isAvailable();
}
