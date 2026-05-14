package com.javaee.common.config.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 基础安全配置类
 * 提供通用的安全设置，供各服务继承使用
 */
@Configuration
@EnableWebSecurity
public class BaseSecurityConfig {

    protected final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 构造函数注入JWT认证过滤器
     * @param jwtAuthenticationFilter JWT认证过滤器
     */
    public BaseSecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * 应用基础安全配置
     * @param http HttpSecurity
     * @throws Exception 异常
     */
    protected void applyBaseSecurityConfig(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF保护，适合API服务
            .csrf().disable()
            // 使用无状态会话管理
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            // 添加JWT认证过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    }

}
