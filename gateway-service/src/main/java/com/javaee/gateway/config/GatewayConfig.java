package com.javaee.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 网关路由配置
 * 支持服务发现模式（lb://）和直连模式（http://）
 */
@Configuration
public class GatewayConfig {

    @Value("${gateway.route.user-uri:lb://user}")
    private String userServiceUri;

    @Value("${gateway.route.file-uri:lb://file}")
    private String fileServiceUri;

    @Value("${gateway.route.ai-uri:lb://ai}")
    private String aiServiceUri;

    @Value("${gateway.route.ai-ws-uri:lb:ws://ai}")
    private String aiWebSocketUri;

    @Value("${gateway.route.document-uri:lb://document}")
    private String documentServiceUri;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // 用户服务路由
                .route("user", r -> r.path("/api/users/**")
                        .uri(userServiceUri))
                // 文件服务路由
                .route("file", r -> r.path("/api/files/**")
                        .uri(fileServiceUri))
                // AI服务路由
                .route("ai", r -> r.path("/api/ai/**")
                        .uri(aiServiceUri))
                // Agent/Skill 独立接口路由
                .route("ai-skills", r -> r.path("/api/skills/**")
                        .uri(aiServiceUri))
                // Agent 工作台 WebSocket 路由
                .route("ai-agent-ws", r -> r.path("/ws/agent/**")
                        .uri(aiWebSocketUri))
                // 文档服务路由
                .route("document", r -> r.path("/api/documents/**")
                        .uri(documentServiceUri))
                .build();
    }

}
