package com.javaee.common.config;

import feign.Logger;
import feign.Request;
import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * @author qxk
 * @description: Feign全局配置（超时/拦截）
 */
@Configuration
public class FeignGlobalConfig {

    /**
     * 配置Feign日志级别
     * @return Logger实例
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    /**
     * 配置Feign超时时间
     * @return Request.Options实例
     */
    @Bean
    public Request.Options feignOptions() {
        return new Request.Options(10, TimeUnit.SECONDS, 60, TimeUnit.SECONDS, true);
    }

    /**
     * 配置Feign重试策略
     * @return Retryer实例
     */
    @Bean
    public Retryer feignRetryer() {
        return new Retryer.Default(100, 1000, 3);
    }
}
