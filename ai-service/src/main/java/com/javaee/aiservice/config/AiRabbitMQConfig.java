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
 * RabbitMQ resources for asynchronous AI model jobs.
 */
@Configuration
public class AiRabbitMQConfig {

    public static final String AI_EXCHANGE = "ai.exchange";
    public static final String AI_MODEL_REQUEST_QUEUE = "ai.model.request.queue";
    public static final String AI_MODEL_REQUEST_ROUTING_KEY = "ai.model.request";

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
