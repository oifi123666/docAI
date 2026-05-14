package com.javaee.common.utils;

import com.javaee.common.constant.CommonConstant;
import com.javaee.common.constant.ErrorCodeEnum;
import com.javaee.common.exception.TokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author qxk
 * @description: JWT工具（生成/解析/验证）
 */
public class JwtUtils {

    private static final String SECRET_KEY = "docAI-secret-key-for-jwt-token-generation-and-validation-2024";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    private static final long TOKEN_EXPIRATION = 30 * 60 * 1000;
    private static final long REFRESH_TOKEN_EXPIRATION = 7 * 24 * 60 * 60 * 1000;

    /**
     * 生成访问令牌
     * @param userId 用户ID
     * @param username 用户名
     * @return 令牌
     */
    public static String generateToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CommonConstant.TOKEN_CLAIM_USER_ID, userId);
        claims.put(CommonConstant.TOKEN_CLAIM_USERNAME, username);
        return generateToken(claims, TOKEN_EXPIRATION);
    }

    /**
     * 生成访问令牌
     * @param userId 用户ID
     * @param username 用户名
     * @param role 角色
     * @return 令牌
     */
    public static String generateToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CommonConstant.TOKEN_CLAIM_USER_ID, userId);
        claims.put(CommonConstant.TOKEN_CLAIM_USERNAME, username);
        claims.put(CommonConstant.TOKEN_CLAIM_ROLE, role);
        return generateToken(claims, TOKEN_EXPIRATION);
    }

    /**
     * 生成刷新令牌
     * @param userId 用户ID
     * @return 刷新令牌
     */
    public static String generateRefreshToken(Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CommonConstant.TOKEN_CLAIM_USER_ID, userId);
        return generateToken(claims, REFRESH_TOKEN_EXPIRATION);
    }

    /**
     * 生成令牌
     * @param claims 声明
     * @param expiration 过期时间
     * @return 令牌
     */
    private static String generateToken(Map<String, Object> claims, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(KEY)
                .compact();
    }

    /**
     * 解析令牌
     * @param token 令牌
     * @return 声明
     */
    public static Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            throw new TokenException(ErrorCodeEnum.TOKEN_ERROR);
        }
    }

    /**
     * 验证令牌
     * @param token 令牌
     * @return 是否有效
     */
    public static boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取用户ID
     * @param token 令牌
     * @return 用户ID
     */
    public static Long getUserId(String token) {
        Claims claims = parseToken(token);
        return claims.get(CommonConstant.TOKEN_CLAIM_USER_ID, Long.class);
    }

    /**
     * 获取用户名
     * @param token 令牌
     * @return 用户名
     */
    public static String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims.get(CommonConstant.TOKEN_CLAIM_USERNAME, String.class);
    }

    /**
     * 获取角色
     * @param token 令牌
     * @return 角色
     */
    public static String getRole(String token) {
        Claims claims = parseToken(token);
        return claims.get(CommonConstant.TOKEN_CLAIM_ROLE, String.class);
    }

    /**
     * 检查令牌是否过期
     * @param token 令牌
     * @return 是否过期
     */
    public static boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 从请求头中提取令牌
     * @param authHeader 认证头
     * @return 令牌
     */
    public static String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith(CommonConstant.TOKEN_PREFIX)) {
            return authHeader.substring(CommonConstant.TOKEN_PREFIX.length());
        }
        throw new TokenException(ErrorCodeEnum.TOKEN_ERROR);
    }
}
