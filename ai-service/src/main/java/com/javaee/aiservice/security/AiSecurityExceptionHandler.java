package com.javaee.aiservice.security;

import com.javaee.common.constant.ErrorCodeEnum;
import com.javaee.common.model.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Converts ai-service authorization failures to a stable API response.
 */
@RestControllerAdvice(basePackages = "com.javaee.aiservice")
public class AiSecurityExceptionHandler {

    @ExceptionHandler(SecurityException.class)
    public Result<Void> handleSecurityException(SecurityException e) {
        return Result.fail(ErrorCodeEnum.FORBIDDEN.getCode(), e.getMessage());
    }
}
