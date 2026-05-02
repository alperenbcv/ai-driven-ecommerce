package com.ecommerce.product.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitMQConfig {

    public static final String PRODUCT_EXCHANGE = "product.exchange";
    public static final String PRODUCT_QUEUE = "product.queue";

    public static final String ROUTING_PRODUCT_CREATED = "product.created";
    public static final String ROUTING_PRODUCT_UPDATED = "product.updated";
    public static final String ROUTING_PRODUCT_DELETED = "product.deleted";

    @Bean
    public TopicExchange productExchange() {
        return new TopicExchange(PRODUCT_EXCHANGE, true, false);
    }

    @Bean
    public Queue productQueue() {
        return QueueBuilder.durable(PRODUCT_QUEUE).build();
    }

    @Bean
    public Binding productBinding() {
        return BindingBuilder
            .bind(productQueue())
            .to(productExchange())
            .with("product.#");
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
