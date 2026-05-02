package com.ecommerce.recommendation.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean TopicExchange orderExchange() { return new TopicExchange("order.exchange", true, false); }

    @Bean
    public Queue recommendationQueue() { return QueueBuilder.durable("recommendation.order.queue").build(); }

    @Bean
    public Binding recommendationBinding() {
        // Sipariş teslim edildiğinde (DELIVERED) graafı güncelle
        return BindingBuilder.bind(recommendationQueue()).to(orderExchange()).with("order.delivered.recommendation");
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() { return new Jackson2JsonMessageConverter(); }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(messageConverter());
        return t;
    }
}
