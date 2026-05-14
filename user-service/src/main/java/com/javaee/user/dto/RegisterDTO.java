package com.javaee.user.dto;

import lombok.Data;

/**
 * @author qxk
 * @description: 注册请求DTO
 */
@Data
public class RegisterDTO {
    // 用户名
    private String username;

    // 密码
    private String password;

    // 邮箱
    private String email;

    // 手机号
    private String phone;
}
