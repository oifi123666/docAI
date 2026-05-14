package com.javaee.aiservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "HTML PPT生成请求")
public class HtmlPptRequest {
    
    @Schema(description = "PPT大纲内容（一行一条，换行分隔）", example = "产品介绍\n核心功能\n技术架构\n演示展示\n总结展望")
    private String outline;
    
    @Schema(description = "主题（支持多种主题，默认：tokyo-night）\n" +
                     "深色主题：tokyo-night, dracula, catppuccin-mocha, nord, gruvbox-dark, rose-pine, cyberpunk-neon\n" +
                     "浅色主题：minimal-white, corporate-clean, pitch-deck-vc, academic-paper, swiss-grid, japanese-minimal, soft-pastel, xiaohongshu-white, solarized-light, catppuccin-latte\n" +
                     "特色主题：aurora, rainbow-gradient, blueprint, terminal-green, glassmorphism, vaporwave, y2k-chrome, bauhaus, memphis-pop, retro-tv, news-broadcast, engineering-whiteprint", 
           example = "tokyo-night")
    private String theme;
    
    @Schema(description = "PPT标题", example = "我的演示文稿")
    private String title;
    
    @Schema(description = "AI模型（可选，默认qwen-plus），支持：qwen-plus, qwen-max, qwen-turbo, deepseek-v3.2", example = "qwen-plus")
    private String model;

    public String getOutline() {
        return outline;
    }

    public void setOutline(String outline) {
        this.outline = outline;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
