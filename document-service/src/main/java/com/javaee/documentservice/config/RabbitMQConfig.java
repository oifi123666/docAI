package com.javaee.documentservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String FILE_EXCHANGE = "file.exchange";
    public static final String FILE_UPLOAD_QUEUE = "file.upload.queue";
    public static final String FILE_UPLOAD_ROUTING_KEY = "file.upload";

    public static final String COLLABORATE_EXCHANGE = "collaborate.exchange";
    public static final String COLLABORATE_EDIT_QUEUE = "collaborate.edit.queue";
    public static final String COLLABORATE_EDIT_ROUTING_KEY = "collaborate.edit";

    @Bean
    public Queue fileUploadQueue() {
        return new Queue(FILE_UPLOAD_QUEUE, true);
    }

    @Bean
    public TopicExchange fileExchange() {
        return new TopicExchange(FILE_EXCHANGE, true, false);
    }

    @Bean
    public Binding fileUploadBinding() {
        return BindingBuilder
                .bind(fileUploadQueue())
                .to(fileExchange())
                .with(FILE_UPLOAD_ROUTING_KEY);
    }

    @Bean
    public Queue collaborateEditQueue() {
        return new Queue(COLLABORATE_EDIT_QUEUE, true);
    }

    @Bean
    public TopicExchange collaborateExchange() {
        return new TopicExchange(COLLABORATE_EXCHANGE, true, false);
    }

    @Bean
    public Binding collaborateEditBinding() {
        return BindingBuilder
                .bind(collaborateEditQueue())
                .to(collaborateExchange())
                .with(COLLABORATE_EDIT_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}