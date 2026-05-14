package com.javaee.common.model;

import com.javaee.common.constant.ErrorCodeEnum;

import java.io.Serializable;

/**
 * @author qxk
 * @description: 全局统一返回结果（核心）
 */
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    // 状态码
    private Integer code;

    // 消息
    private String message;

    // 数据
    private T data;

    // 时间戳
    private Long timestamp;

    // 构造方法
    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    // 静态方法
    /**
     * 成功
     * @param data 数据
     * @param <T> 数据类型
     * @return Result
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ErrorCodeEnum.SUCCESS.getCode(), ErrorCodeEnum.SUCCESS.getMessage(), data);
    }

    /**
     * 成功
     * @param <T> 数据类型
     * @return Result
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 成功
     * @param message 消息
     * @param data 数据
     * @param <T> 数据类型
     * @return Result
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ErrorCodeEnum.SUCCESS.getCode(), message, data);
    }

    /**
     * 失败
     * @param code 状态码
     * @param message 消息
     * @param <T> 数据类型
     * @return Result
     */
    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 失败
     * @param errorCode 错误码枚举
     * @param <T> 数据类型
     * @return Result
     */
    public static <T> Result<T> fail(ErrorCodeEnum errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /**
     * 失败
     * @param errorCode 错误码枚举
     * @param message 消息
     * @param <T> 数据类型
     * @return Result
     */
    public static <T> Result<T> fail(ErrorCodeEnum errorCode, String message) {
        return new Result<>(errorCode.getCode(), message, null);
    }

    /**
     * 失败
     * @param message 消息
     * @param <T> 数据类型
     * @return Result
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(ErrorCodeEnum.BUSINESS_ERROR.getCode(), message, null);
    }

    // getter and setter
    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    // toString
    @Override
    public String toString() {
        return "Result{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", data=" + data +
                ", timestamp=" + timestamp +
                '}';
    }
}

