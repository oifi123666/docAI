package com.javaee.documentservice.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Forward user identity to internal service calls.
 */
@Configuration
public class FeignAuthForwardConfig {

    @Bean
    public RequestInterceptor authForwardInterceptor() {
        return template -> {
            if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
                return;
            }
            String authorization = attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization != null && !authorization.isBlank()) {
                template.header(HttpHeaders.AUTHORIZATION, authorization);
            }
            String userId = attributes.getRequest().getHeader("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                template.header("X-User-Id", userId);
            }
        };
    }
}
