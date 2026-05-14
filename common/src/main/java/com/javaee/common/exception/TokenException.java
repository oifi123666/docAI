package com.javaee.common.exception;

import com.javaee.common.constant.ErrorCodeEnum;
import lombok.Getter;

/**
 * @author qxk
 * @description: 自定义令牌异常（鉴权专用）
 * 用于处理用户认证和授权相关的异常，如令牌过期、令牌无效等
 */
@Getter
public class TokenException extends RuntimeException {
    private Integer code;
    private String message;

    /**
     * 构造函数：仅指定错误消息
     * @param message 错误消息
     */
    public TokenException(String message) {
        super(message);
        this.code = ErrorCodeEnum.UNAUTHORIZED.getCode();
        this.message = message;
    }

    /**
     * 构造函数：指定错误码和错误消息
     * @param code 错误码
     * @param message 错误消息
     */
    public TokenException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * 构造函数：使用错误码枚举
     * @param errorCodeEnum 错误码枚举
     */
    public TokenException(ErrorCodeEnum errorCodeEnum) {
        super(errorCodeEnum.getMessage());
        this.code = errorCodeEnum.getCode();
        this.message = errorCodeEnum.getMessage();
    }
}
