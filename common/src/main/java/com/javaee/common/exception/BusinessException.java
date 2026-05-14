package com.javaee.common.exception;

import com.javaee.common.constant.ErrorCodeEnum;
import lombok.Getter;

/**
 * @author qxk
 * @description: 自定义业务异常（可控）
 * 用于处理业务逻辑中的可控异常，如数据不存在、业务规则不满足等
 */
@Getter
public class BusinessException extends RuntimeException {
    private Integer code;
    private String message;

    /**
     * 构造函数：仅指定错误消息
     * @param message 错误消息
     */
    public BusinessException(String message) {
        super(message);
        this.code = ErrorCodeEnum.BUSINESS_ERROR.getCode();
        this.message = message;
    }

    /**
     * 构造函数：指定错误码和错误消息
     * @param code 错误码
     * @param message 错误消息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * 构造函数：使用错误码枚举
     * @param errorCodeEnum 错误码枚举
     */
    public BusinessException(ErrorCodeEnum errorCodeEnum) {
        super(errorCodeEnum.getMessage());
        this.code = errorCodeEnum.getCode();
        this.message = errorCodeEnum.getMessage();
    }
}
