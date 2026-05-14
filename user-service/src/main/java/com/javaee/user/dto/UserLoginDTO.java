package com.javaee.user.dto;

import lombok.Data;

/**
 * @author qxk
 * @description: 用户登录DTO
 */
@Data
public class UserLoginDTO {

    private String username;

    private String password;
}
