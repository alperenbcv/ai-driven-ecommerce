package com.ecommerce.search.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    TopicExchange productExchange() {
        return new TopicExchange("product.exchange", true, false);
    }

    @Bean
    public Queue searchIndexQueue() {
        return QueueBuilder.durable("search.product.index.queue").build();
    }

    @Bean
    public Queue searchDeleteQueue() {
        return QueueBuilder.durable("search.product.delete.queue").build();
    }

    /** Yalnızca oluşturma / güncelleme — {@code product.deleted} buraya düşmez */
    @Bean
    public Binding searchIndexCreatedBinding() {
        return BindingBuilder.bind(searchIndexQueue()).to(productExchange()).with("product.created");
    }

    @Bean
    public Binding searchIndexUpdatedBinding() {
        return BindingBuilder.bind(searchIndexQueue()).to(productExchange()).with("product.updated");
    }

    @Bean
    public Binding searchDeleteBinding() {
        return BindingBuilder.bind(searchDeleteQueue()).to(productExchange()).with("product.deleted");
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(messageConverter());
        return t;
    }
}
