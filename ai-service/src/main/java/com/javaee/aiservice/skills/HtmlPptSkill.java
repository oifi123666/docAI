package com.javaee.aiservice.skills;

import com.javaee.aiservice.agent.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

public class HtmlPptSkill implements Skill {

    private static final Logger log = LoggerFactory.getLogger(HtmlPptSkill.class);

    private final ChatService chatService;

    public HtmlPptSkill(ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public String getName() {
        return "HTML PPT Skill";
    }

    @Override
    public String getDescription() {
        return "根据大纲生成专业的HTML演示文稿，支持多种主题和动画效果，按S键进入演讲者模式，支持逐字稿";
    }

    @Override
    public Object execute(Object... parameters) {
        String outline = parameters.length > 0 && parameters[0] != null ? (String) parameters[0] : "";
        String theme = parameters.length > 1 && parameters[1] != null ? (String) parameters[1] : "tokyo-night";
        String title = parameters.length > 2 && parameters[2] != null ? (String) parameters[2] : "演示文稿";
        String model = parameters.length > 3 && parameters[3] != null ? (String) parameters[3] : null;

        try {
            return generateHtmlPpt(outline, theme, title, model);
        } catch (Exception e) {
            throw new RuntimeException("生成HTML PPT失败: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> generateHtmlPpt(String outline, String theme, String title, String model) throws Exception {
        List<Slide> slides = generateSlidesWithAI(outline, title, model);
        String htmlContent = buildHtml(slides, theme, title);
        String fileName = "ppt-" + UUID.randomUUID().toString() + ".html";
        String tempDir = System.getProperty("java.io.tmpdir");
        String filePath = tempDir + java.io.File.separator + fileName;

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath), StandardCharsets.UTF_8)) {
            writer.write(htmlContent);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("fileName", fileName);
        result.put("filePath", filePath);
        result.put("title", title);
        result.put("theme", theme);
        result.put("model", model);
        result.put("slideCount", slides.size());
        result.put("htmlContent", htmlContent);
        
        return result;
    }

    private List<Slide> generateSlidesWithAI(String outline, String title, String model) throws Exception {
        log.info("开始生成PPT，接收到的大纲：[{}]，使用模型：[{}]", outline, model);
        List<Slide> slides = new ArrayList<>();
        
        slides.add(new Slide("cover", title, "", "", generateCoverNotes(title)));
        
        try {
            String prompt = String.format("""
                请根据以下大纲生成一个 HTML PPT 演示文稿的内容结构，包括每一页的内容和演讲者逐字稿。
                
                要求：
                1. 每页有一个标题
                2. 每页有 2-4 个要点（列表形式，用 <ul> 和 <li> 标签）
                3. 每页要有 150-300 字的逐字稿（口语化，关键词用 <strong> 加粗）
                4. 用以下格式返回，每页一行：
                   页面标题 | 要点1 | 要点2 | 要点3 | 逐字稿内容
                5. 只返回内容，不要其他说明
                
                大纲：
                %s
                """, outline);
            
            log.info("调用AI生成内容，提示词：[{}]", prompt);
            String aiResponse;
            if (model != null && !model.isEmpty()) {
                aiResponse = chatService.callChatApiWithModelCode(prompt, model);
            } else {
                aiResponse = chatService.callChatApi(prompt);
            }
            log.info("AI响应结果：[{}]", aiResponse);
            
            String[] lines = aiResponse.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                if (line.contains("|")) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 2) {
                        String slideTitle = parts[0].trim();
                        
                        StringBuilder content = new StringBuilder();
                        content.append("<ul>");
                        
                        String notes = "";
                        
                        for (int i = 1; i < parts.length; i++) {
                            String part = parts[i].trim();
                            if (!part.isEmpty()) {
                                if (i == parts.length - 1 && part.length() > 50) {
                                    notes = part;
                                } else {
                                    content.append("<li>").append(escapeHtml(part)).append("</li>");
                                }
                            }
                        }
                        content.append("</ul>");
                        
                        if (notes.isEmpty()) {
                            notes = "这一页主要讲 " + slideTitle + "，接下来详细介绍...";
                        }
                        
                        log.info("解析出一页：标题[{}]，内容[{}]", slideTitle, content);
                        slides.add(new Slide("content", slideTitle, "", content.toString(), notes));
                    }
                }
            }
        } catch (Exception e) {
            log.error("调用AI生成内容失败，使用fallback方法：", e);
        }
        
        if (slides.size() <= 1) {
            log.info("AI没有生成有效内容，使用fallback方法解析");
            slides = parseOutlineFallback(outline, title);
        }
        
        slides.add(new Slide("thanks", "谢谢观看", "Thank you for watching", "", generateThanksNotes()));
        log.info("最终生成的PPT页数：{}", slides.size());
        
        return slides;
    }

    private String generateCoverNotes(String title) {
        return "<p>大家好，欢迎来到今天的分享。今天我们来讲讲<strong>" + title + "</strong>。</p>" +
               "<p>希望今天的内容能给大家一些启发，有问题随时提问。</p>" +
               "<p>好，我们开始今天的内容。</p>";
    }

    private String generateThanksNotes() {
        return "<p>以上就是今天分享的全部内容了。</p>" +
               "<p>希望对大家有帮助。</p>" +
               "<p>现在是问答环节，有什么问题欢迎随时提问。谢谢大家！</p>";
    }

    private List<Slide> parseOutlineFallback(String outline, String title) {
        log.info("使用fallback方法解析，原始大纲：[{}]", outline);
        List<Slide> slides = new ArrayList<>();
        
        slides.add(new Slide("cover", title, "", "", generateCoverNotes(title)));
        
        if (outline == null || outline.trim().isEmpty()) {
            slides.add(new Slide("content", "欢迎", "", "<ul><li>这是一个演示文稿</li><li>用键盘左右键切换</li><li>按S进入演讲者模式</li></ul>", "这一页主要介绍一下演示文稿的基本操作..."));
            slides.add(new Slide("thanks", "谢谢观看", "Thank you for watching", "", generateThanksNotes()));
            return slides;
        }
        
        List<String> items = new ArrayList<>();
        String[] lines = outline.split("\n");
        
        if (lines.length == 1 && lines[0].contains("\\n")) {
            lines = lines[0].split("\\\\n");
        }
        
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty()) {
                items.add(line);
            }
        }
        
        log.info("解析出的条目数量：{}，内容：{}", items.size(), items);
        
        if (!items.isEmpty()) {
            int slideSize = 3;
            for (int i = 0; i < items.size(); i += slideSize) {
                StringBuilder content = new StringBuilder();
                content.append("<ul>");
                
                for (int j = i; j < i + slideSize && j < items.size(); j++) {
                    String item = items.get(j);
                    item = item.replaceAll("^[-*•]\\s+", "")
                               .replaceAll("^\\d+[.、)）]\\s*", "")
                               .replaceAll("^Slide\\s*\\d*[:：]\\s*", "");
                    content.append("<li>").append(escapeHtml(item)).append("</li>");
                }
                content.append("</ul>");
                
                String slideTitle = items.get(i);
                String notes = "这一页主要讲 " + slideTitle + "，接下来详细介绍...";
                
                log.info("创建幻灯片：标题[{}]，内容[{}]", slideTitle, content);
                slides.add(new Slide("content", slideTitle, "", content.toString(), notes));
            }
        }
        
        slides.add(new Slide("thanks", "谢谢观看", "Thank you for watching", "", generateThanksNotes()));
        
        log.info("fallback方法生成的幻灯片数量：{}", slides.size());
        return slides;
    }

    private String readResource(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("读取资源失败 {}: {}", path, e.getMessage());
            return "";
        }
    }

    private String buildHtml(List<Slide> slides, String theme, String title) {
        String fontsCss = readResource("static/ppt-templates/assets/fonts.css");
        String baseCss = readResource("static/ppt-templates/assets/base.css");
        String themeCss = readResource("static/ppt-templates/assets/themes/" + theme + ".css");
        String animationsCss = readResource("static/ppt-templates/assets/animations/animations.css");
        String runtimeJs = readResource("static/ppt-templates/assets/runtime.js");
        String fxRuntimeJs = readResource("static/ppt-templates/assets/animations/fx-runtime.js");

        String[] themes = {"tokyo-night", "dracula", "catppuccin-mocha", "nord", "corporate-clean", "minimal-white", "cyberpunk-neon", "aurora"};
        String themesStr = String.join(",", themes);

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"zh-CN\" data-themes=\"").append(themesStr).append("\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"utf-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n");
        html.append("    <title>").append(title).append("</title>\n");

        if (!fontsCss.isEmpty()) html.append("<style>\n").append(fontsCss).append("</style>\n");
        if (!baseCss.isEmpty()) html.append("<style>\n").append(baseCss).append("</style>\n");
        if (!themeCss.isEmpty()) html.append("<style>\n").append(themeCss).append("</style>\n");
        if (!animationsCss.isEmpty()) html.append("<style>\n").append(animationsCss).append("</style>\n");

        html.append("</head>\n");
        html.append("<body>\n");
        html.append("<div class=\"deck\">\n");
        
        for (int i = 0; i < slides.size(); i++) {
            Slide slide = slides.get(i);
            html.append("\n");
            html.append("    <!-- ============ ").append(i + 1).append(". ").append(slide.type.toUpperCase()).append(" ============ -->\n");
            html.append("    <section class=\"slide\" data-title=\"").append(escapeHtml(slide.title)).append("\">\n");
            
            if ("cover".equals(slide.type)) {
                html.append("        <p class=\"kicker\">欢迎 · 介绍</p>\n");
                html.append("        <h1 class=\"h1 anim-fade-up\" data-anim=\"fade-up\">").append(escapeHtml(slide.title)).append("</h1>\n");
                html.append("        <p class=\"lede mt-m\">按 <span class=\"mono\">S</span> 进入演讲者视图 · <span class=\"mono\">T</span> 切换主题 · <span class=\"mono\">← →</span> 翻页</p>\n");
                html.append("        <div class=\"deck-footer\">\n");
                html.append("            <span class=\"mono\">#presenter #tech-talk</span>\n");
                html.append("            <span class=\"slide-number\" data-current=\"").append(i + 1).append("\" data-total=\"").append(slides.size()).append("\"></span>\n");
                html.append("        </div>\n");
            } else if ("thanks".equals(slide.type)) {
                html.append("        <p class=\"kicker\">总结 · 感谢</p>\n");
                html.append("        <h1 class=\"h1 anim-fade-up\" data-anim=\"fade-up\">").append(escapeHtml(slide.title)).append("</h1>\n");
                html.append("        <p class=\"lede mt-m\">Q&amp;A · 有问题随时提问</p>\n");
                html.append("        <div class=\"deck-footer\">\n");
                html.append("            <span class=\"mono\">#thanks #Q&A</span>\n");
                html.append("            <span class=\"slide-number\" data-current=\"").append(i + 1).append("\" data-total=\"").append(slides.size()).append("\"></span>\n");
                html.append("        </div>\n");
            } else {
                html.append("        <p class=\"kicker\">// content · ").append(i + 1).append("/").append(slides.size()).append("</p>\n");
                html.append("        <h2 class=\"h2 anim-fade-up\" data-anim=\"fade-up\">").append(escapeHtml(slide.title)).append("</h2>\n");
                html.append("        <div class=\"stack mt-l\">\n");
                html.append("            <div class=\"content\">").append(slide.content).append("</div>\n");
                html.append("        </div>\n");
                html.append("        <div class=\"deck-footer\">\n");
                html.append("            <span class=\"slide-number\" data-current=\"").append(i + 1).append("\" data-total=\"").append(slides.size()).append("\"></span>\n");
                html.append("        </div>\n");
            }
            
            html.append("        <aside class=\"notes\">\n");
            html.append("            <p>").append(slide.notes).append("</p>\n");
            html.append("        </aside>\n");
            
            html.append("    </section>\n");
        }
        
        html.append("\n</div>\n");
        html.append("\n<div style=\"position:fixed;bottom:12px;left:12px;font-size:11px;color:#484f5866;z-index:100;pointer-events:none\">\n");
        html.append("  S 演讲者视图 · T 切换主题 · ← → 翻页 · F 全屏 · O 总览 · R 重置计时\n");
        html.append("</div>\n");
        
        if (!fxRuntimeJs.isEmpty()) html.append("\n<script>\n").append(fxRuntimeJs).append("</script>\n");
        if (!runtimeJs.isEmpty()) html.append("\n<script>\n").append(runtimeJs).append("</script>\n");
        
        html.append("</body>\n");
        html.append("</html>");
        
        return html.toString();
    }
    
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#039;");
    }

    static class Slide {
        String type;
        String title;
        String subtitle;
        String content;
        String notes;

        Slide(String type, String title, String subtitle, String content, String notes) {
            this.type = type;
            this.title = title;
            this.subtitle = subtitle;
            this.content = content;
            this.notes = notes;
        }
    }
}
