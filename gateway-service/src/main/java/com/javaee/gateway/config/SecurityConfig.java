package com.javaee.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * 安全配置
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            // 禁用CSRF保护
            .csrf(csrf -> csrf.disable())
            // 配置路径权限
            .authorizeExchange(exchanges -> exchanges
                // 公开接口
                .pathMatchers("/api/users/register", "/api/users/login").permitAll()
                // 其他接口需要认证
                .anyExchange().permitAll()
            );
        return http.build();
    }

}