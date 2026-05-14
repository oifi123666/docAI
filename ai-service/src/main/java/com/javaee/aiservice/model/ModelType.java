package com.javaee.aiservice.model;

public enum ModelType {
    QWEN_PLUS("qwen-plus", "通义千问Plus", "dashscope"),
    QWEN_MAX("qwen-max", "通义千问Max", "dashscope"),
    QWEN_TURBO("qwen-turbo", "通义千问Turbo", "dashscope"),
    DEEPSEEK_V3("deepseek-v3.2", "DeepSeek V3.2", "dashscope");

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
        return QWEN_PLUS;
    }
}
