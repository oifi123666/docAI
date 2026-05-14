package com.javaee.common.config.security;

import com.javaee.common.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * JWT认证过滤器
 * 用于验证请求中的JWT令牌并设置认证上下文
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, jakarta.servlet.FilterChain chain) 
            throws ServletException, IOException {
        try {
            // 从请求头中获取令牌
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                // 提取令牌
                String token = authHeader.substring("Bearer ".length());
                
                // 验证令牌
                if (JwtUtils.validateToken(token)) {
                    // 解析令牌获取用户信息
                    Claims claims = JwtUtils.parseToken(token);
                    Long userId = claims.get("userId", Long.class);
                    String username = claims.get("username", String.class);
                    String role = claims.get("role", String.class);
                    
                    // 创建认证令牌
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userId, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                    );
                    
                    // 设置认证详情
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // 设置安全上下文
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception e) {
            // 令牌验证失败，清除认证上下文
            SecurityContextHolder.clearContext();
        }
        
        // 继续过滤链
        chain.doFilter(request, response);
    }
}
