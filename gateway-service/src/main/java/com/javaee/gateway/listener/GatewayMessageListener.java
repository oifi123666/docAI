package com.javaee.gateway.listener;

import com.javaee.gateway.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 网关服务消息监听器
 */
@Slf4j
@Component
public class GatewayMessageListener {

    /**
     * 处理网关日志消息
     */
    @RabbitListener(queues = RabbitMQConfig.GATEWAY_LOG_QUEUE)
    public void handleGatewayLogMessage(Map<String, Object> message) {
        log.info("=== 收到网关日志消息 ===");
        log.info("消息内容: {}", message);
        log.info("处理时间: {}", LocalDateTime.now());
        
        String path = (String) message.get("path");
        String method = (String) message.get("method");
        String userId = (String) message.get("userId");
        
        log.info("请求路径: {}, 请求方法: {}, 用户ID: {}", path, method, userId);
        
        log.info("网关日志消息处理完成");
    }

    /**
     * 处理网关告警消息
     */
    @RabbitListener(queues = RabbitMQConfig.GATEWAY_ALERT_QUEUE)
    public void handleGatewayAlertMessage(Map<String, Object> message) {
        log.info("=== 收到网关告警消息 ===");
        log.info("消息内容: {}", message);
        log.info("处理时间: {}", LocalDateTime.now());
        
        String alertType = (String) message.get("alertType");
        String description = (String) message.get("description");
        
        log.info("告警类型: {}, 描述: {}", alertType, description);
        
        log.info("网关告警消息处理完成");
    }
}
