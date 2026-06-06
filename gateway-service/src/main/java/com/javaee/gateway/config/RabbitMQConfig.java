package com.javaee.gateway.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置类
 */
@Configuration
public class RabbitMQConfig {

    // 网关服务相关队列
    public static final String GATEWAY_LOG_QUEUE = "gateway.log.queue";
    public static final String GATEWAY_ALERT_QUEUE = "gateway.alert.queue";
    
    // 网关服务相关交换机
    public static final String GATEWAY_EXCHANGE = "gateway.exchange";
    
    // 网关服务相关路由键
    public static final String GATEWAY_LOG_ROUTING_KEY = "gateway.log";
    public static final String GATEWAY_ALERT_ROUTING_KEY = "gateway.alert";

    /**
     * 声明交换机
     */
    @Bean
    public TopicExchange gatewayExchange() {
        return new TopicExchange(GATEWAY_EXCHANGE, true, false);
    }

    /**
     * 声明网关日志队列
     */
    @Bean
    public Queue gatewayLogQueue() {
        return new Queue(GATEWAY_LOG_QUEUE, true, false, false);
    }

    /**
     * 声明网关告警队列
     */
    @Bean
    public Queue gatewayAlertQueue() {
        return new Queue(GATEWAY_ALERT_QUEUE, true, false, false);
    }

    /**
     * 绑定网关日志队列到交换机
     */
    @Bean
    public Binding bindingGatewayLogQueue() {
        return BindingBuilder.bind(gatewayLogQueue()).to(gatewayExchange()).with(GATEWAY_LOG_ROUTING_KEY);
    }

    /**
     * 绑定网关告警队列到交换机
     */
    @Bean
    public Binding bindingGatewayAlertQueue() {
        return BindingBuilder.bind(gatewayAlertQueue()).to(gatewayExchange()).with(GATEWAY_ALERT_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
