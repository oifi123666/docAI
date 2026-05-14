package com.javaee.user.exception;

import lombok.Getter;

/**
 * @author qxk
 * @description: 用户模块异常
 */
@Getter
public class UserException extends RuntimeException {

    private Integer code;

    private String message;

    public UserException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public UserException(String message) {
        super(message);
        this.message = message;
    }
}
