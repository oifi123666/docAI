package com.javaee.user.config;

import com.javaee.common.config.security.BaseSecurityConfig;
import com.javaee.common.config.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * @author qxk
 * @description: 用户服务安全配置
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig extends BaseSecurityConfig {

    /**
     * 构造函数注入JWT认证过滤器
     * @param jwtAuthenticationFilter JWT认证过滤器
     */
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        super(jwtAuthenticationFilter);
    }

    /**
     * 密码加密器
     * @return BCryptPasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * BCryptPasswordEncoder实例（用于依赖注入）
     * @return BCryptPasswordEncoder
     */
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 配置SecurityFilterChain
     * @param http HttpSecurity
     * @return SecurityFilterChain
     * @throws Exception 异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 应用基础安全配置
        applyBaseSecurityConfig(http);
        
        http
            // 授权配置
            .authorizeHttpRequests(authorize -> authorize
                // 确保Swagger相关路径的匹配规则在最前面
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/v3/api-docs").permitAll()
                // 允许登录和注册接口免认证访问
                .requestMatchers("/api/users/login", "/api/users/register").permitAll()
                // 允许获取用户信息接口免认证访问（用于测试）
                .requestMatchers("/api/users/**").permitAll()
                // 允许静态资源访问
                .requestMatchers("/static/**", "/public/**").permitAll()
                // 允许健康检查等端点访问
                .requestMatchers("/actuator/**").permitAll()
                // 允许错误处理端点访问
                .requestMatchers("/error").permitAll()
                // 其他接口需要认证
                .anyRequest().authenticated()
            );

        return http.build();
    }
}