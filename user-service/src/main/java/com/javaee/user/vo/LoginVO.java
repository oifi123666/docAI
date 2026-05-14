package com.javaee.user.vo;

import lombok.Data;

/**
 * @author qxk
 * @description: 登录响应VO
 */
@Data
public class LoginVO {
    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 刷新令牌
     */
    private String refreshToken;

    /**
     * 用户信息
     */
    private UserVO user;
}
