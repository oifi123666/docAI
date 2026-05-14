package com.javaee.aiservice.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置类
 * 配置消息队列和交换机
 */
@Configuration
public class RabbitMQConfig {

    public static final String AI_EXCHANGE = "ai.exchange";
    public static final String AI_TASK_QUEUE = "ai.task.queue";
    public static final String AI_ALERT_QUEUE = "ai.alert.queue";

    /**
     * 创建交换机
     */
    @Bean
    public Exchange aiExchange() {
        return ExchangeBuilder.directExchange(AI_EXCHANGE).durable(true).build();
    }

    /**
     * 创建任务队列
     */
    @Bean
    public Queue aiTaskQueue() {
        return QueueBuilder.durable(AI_TASK_QUEUE).build();
    }

    /**
     * 创建告警队列
     */
    @Bean
    public Queue aiAlertQueue() {
        return QueueBuilder.durable(AI_ALERT_QUEUE).build();
    }

    /**
     * 绑定任务队列
     */
    @Bean
    public Binding taskBinding(Exchange aiExchange, Queue aiTaskQueue) {
        return BindingBuilder.bind(aiTaskQueue).to(aiExchange).with("task").noargs();
    }

    /**
     * 绑定告警队列
     */
    @Bean
    public Binding alertBinding(Exchange aiExchange, Queue aiAlertQueue) {
        return BindingBuilder.bind(aiAlertQueue).to(aiExchange).with("alert").noargs();
    }
}
