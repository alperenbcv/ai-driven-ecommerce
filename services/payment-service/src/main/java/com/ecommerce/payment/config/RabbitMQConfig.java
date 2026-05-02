package com.ecommerce.payment.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String PAYMENT_EXCHANGE = "payment.exchange";
    public static final String PAYMENT_SUCCESS_KEY = "payment.success";
    public static final String PAYMENT_FAILED_KEY = "payment.failed";

    public static final String PAYMENT_INITIATE_QUEUE = "payment.initiate.queue";

    @Bean TopicExchange orderExchange() { return new TopicExchange("order.exchange", true, false); }
    @Bean TopicExchange paymentExchange() { return new TopicExchange(PAYMENT_EXCHANGE, true, false); }

    @Bean
    public Queue paymentInitiateQueue() {
        return QueueBuilder.durable(PAYMENT_INITIATE_QUEUE).build();
    }

    @Bean
    public Binding paymentInitiateBinding() {
        return BindingBuilder.bind(paymentInitiateQueue()).to(orderExchange()).with("payment.initiate");
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
