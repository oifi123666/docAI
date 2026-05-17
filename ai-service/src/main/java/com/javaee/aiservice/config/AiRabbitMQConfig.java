package com.javaee.aiservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 服务统一 RabbitMQ 配置。
 * 当前只启用异步模型任务队列；后续 Agent 任务、AIOps 告警也应复用该 TopicExchange。
 */
@Configuration
public class AiRabbitMQConfig {

    public static final String AI_EXCHANGE = "ai.exchange";

    public static final String AI_MODEL_REQUEST_QUEUE = "ai.model.request.queue";
    public static final String AI_MODEL_REQUEST_ROUTING_KEY = "ai.model.request";

    public static final String AI_AGENT_TASK_ROUTING_KEY = "ai.agent.task";
    public static final String AI_ALERT_ROUTING_KEY = "ai.alert";

    @Bean
    public TopicExchange aiExchange() {
        return new TopicExchange(AI_EXCHANGE, true, false);
    }

    @Bean
    public Queue aiModelRequestQueue() {
        return new Queue(AI_MODEL_REQUEST_QUEUE, true, false, false);
    }

    @Bean
    public Binding aiModelRequestBinding() {
        return BindingBuilder.bind(aiModelRequestQueue())
                .to(aiExchange())
                .with(AI_MODEL_REQUEST_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter aiJackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate aiRabbitTemplate(ConnectionFactory connectionFactory,
                                           Jackson2JsonMessageConverter aiJackson2JsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(aiJackson2JsonMessageConverter);
        return template;
    }
}
