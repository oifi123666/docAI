package com.javaee.common.constant;

/**
 * @author qxk
 * @description: 错误码枚举（200/401/500等）
 */
public enum ErrorCodeEnum {
    // 成功
    SUCCESS(200, "操作成功"),
    
    // 客户端错误
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权，请重新登录"),
    FORBIDDEN(403, "拒绝访问"),
    NOT_FOUND(404, "请求资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    CONFLICT(409, "请求冲突"),
    UNPROCESSABLE_ENTITY(422, "请求参数验证失败"),
    
    // 服务端错误
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),
    GATEWAY_TIMEOUT(504, "网关超时"),
    
    // 业务错误
    BUSINESS_ERROR(600, "业务逻辑错误"),
    PARAM_ERROR(601, "参数错误"),
    DATA_ERROR(602, "数据错误"),
    PERMISSION_ERROR(603, "权限错误"),
    TOKEN_ERROR(604, "令牌错误"),
    TOKEN_EXPIRED(605, "令牌过期"),
    USER_NOT_FOUND(606, "用户不存在"),
    USER_EXISTED(607, "用户已存在"),
    PASSWORD_ERROR(608, "密码错误"),
    FILE_ERROR(609, "文件错误"),
    UPLOAD_ERROR(610, "上传失败"),
    DOWNLOAD_ERROR(611, "下载失败"),
    DATABASE_ERROR(612, "数据库错误"),
    REDIS_ERROR(613, "Redis错误"),
    THIRD_PARTY_ERROR(614, "第三方服务错误"),
    
    // AI服务错误
    AI_API_ERROR(700, "AI API调用错误"),
    AI_REQUEST_ERROR(701, "AI请求参数错误"),
    AI_RESPONSE_ERROR(702, "AI响应解析错误"),
    AI_RATE_LIMIT_ERROR(703, "AI API调用频率受限"),
    AI_AUTH_ERROR(704, "AI API认证失败"),
    
    // 系统错误
    SYSTEM_ERROR(900, "系统错误"),
    CONFIG_ERROR(901, "配置错误"),
    NETWORK_ERROR(902, "网络错误"),
    TIMEOUT_ERROR(903, "超时错误");

    private final Integer code;
    private final String message;

    ErrorCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    /**
     * 根据错误码获取错误枚举
     * @param code 错误码
     * @return 错误枚举
     */
    public static ErrorCodeEnum getByCode(Integer code) {
        for (ErrorCodeEnum errorCode : values()) {
            if (errorCode.code.equals(code)) {
                return errorCode;
            }
        }
        return INTERNAL_SERVER_ERROR;
    }

    /**
     * 根据错误码获取错误消息
     * @param code 错误码
     * @return 错误消息
     */
    public static String getMessageByCode(Integer code) {
        ErrorCodeEnum errorCode = getByCode(code);
        return errorCode != null ? errorCode.getMessage() : INTERNAL_SERVER_ERROR.getMessage();
    }
}

