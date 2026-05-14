package com.javaee.user.dto;

import lombok.Data;

/**
 * @author qxk
 * @description: 登录请求DTO
 */
@Data
public class LoginDTO {
    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;
}
