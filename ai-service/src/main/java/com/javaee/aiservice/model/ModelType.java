package com.javaee.aiservice.model;

public enum ModelType {
    QWEN36_PLUS("qwen3.6-plus", "通义千问3.6 Plus", "openai"),
    GLM5("glm-5", "智谱GLM-5", "openai"),
    KIMI_K25("kimi-k2.5", "月之暗面K2.5", "openai"),
    MINIMAX_M25("MiniMax-M2.5", "MiniMax M2.5", "openai");

    private final String code;
    private final String name;
    private final String provider;

    ModelType(String code, String name, String provider) {
        this.code = code;
        this.name = name;
        this.provider = provider;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getProvider() {
        return provider;
    }

    public static ModelType fromCode(String code) {
        for (ModelType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return QWEN36_PLUS;
    }
}
