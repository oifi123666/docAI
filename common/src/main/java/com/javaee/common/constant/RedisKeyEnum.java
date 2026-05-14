package com.javaee.common.constant;

/**
 * @author qxk
 * @description: Redis键名枚举（避免硬编码）
 */
public enum RedisKeyEnum {
    // 用户相关
    USER_TOKEN("user:token:%s", "用户登录令牌"),
    USER_INFO("user:info:%s", "用户信息"),
    USER_PERMISSIONS("user:permissions:%s", "用户权限"),
    USER_ROLES("user:roles:%s", "用户角色"),
    USER_LOGIN_COUNT("user:login:count:%s", "用户登录次数"),
    USER_LOGIN_LOCK("user:login:lock:%s", "用户登录锁定"),
    
    // 缓存相关
    CACHE_PREFIX("cache:%s", "缓存前缀"),
    CACHE_EXPIRE("cache:expire:%s", "缓存过期时间"),
    
    // AI相关
    AI_REQUEST_PREFIX("ai:request:%s", "AI请求前缀"),
    AI_RESPONSE_PREFIX("ai:response:%s", "AI响应前缀"),
    AI_TASK_PREFIX("ai:task:%s", "AI任务前缀"),
    AI_RATE_LIMIT("ai:rate:limit:%s", "AI调用频率限制"),
    
    // 文件相关
    FILE_UPLOAD_LOCK("file:upload:lock:%s", "文件上传锁定"),
    FILE_DOWNLOAD_COUNT("file:download:count:%s", "文件下载次数"),
    
    // 系统相关
    SYSTEM_CONFIG("system:config:%s", "系统配置"),
    SYSTEM_PARAM("system:param:%s", "系统参数"),
    SYSTEM_DICT("system:dict:%s", "系统字典"),
    
    // 限流相关
    RATE_LIMIT_PREFIX("rate:limit:%s", "限流前缀"),
    RATE_LIMIT_COUNT("rate:limit:count:%s", "限流计数"),
    
    // 分布式锁
    DISTRIBUTED_LOCK("lock:%s", "分布式锁"),
    
    // 消息队列
    MESSAGE_QUEUE_PREFIX("mq:%s", "消息队列前缀"),
    MESSAGE_QUEUE_DELAY("mq:delay:%s", "延迟消息队列"),
    
    // 统计相关
    STATISTICS_PREFIX("stats:%s", "统计前缀"),
    STATISTICS_DAILY("stats:daily:%s", "每日统计"),
    STATISTICS_MONTHLY("stats:monthly:%s", "每月统计"),
    
    // 其他
    TEMP_DATA("temp:%s", "临时数据"),
    LOCK_PREFIX("lock:%s", "锁前缀");

    private final String keyPattern;
    private final String description;

    RedisKeyEnum(String keyPattern, String description) {
        this.keyPattern = keyPattern;
        this.description = description;
    }

    public String getKeyPattern() {
        return keyPattern;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 生成完整的Redis键
     * @param params 键参数
     * @return 完整的Redis键
     */
    public String getKey(Object... params) {
        return String.format(keyPattern, params);
    }

    /**
     * 生成完整的Redis键
     * @param param 键参数
     * @return 完整的Redis键
     */
    public String getKey(String param) {
        return String.format(keyPattern, param);
    }
}
