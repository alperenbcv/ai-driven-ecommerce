package com.ecommerce.stock.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String STOCK_RESERVE_QUEUE = "stock.reserve.queue";
    public static final String STOCK_RELEASE_QUEUE = "stock.release.queue";
    public static final String STOCK_CONFIRM_QUEUE = "stock.confirm.queue";

    public static final String STOCK_EXCHANGE = "stock.exchange";
    public static final String STOCK_RESERVED_KEY = "stock.reserved";
    public static final String STOCK_FAILED_KEY = "stock.failed";
    public static final String LOW_STOCK_KEY = "stock.low";

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange stockExchange() {
        return new TopicExchange(STOCK_EXCHANGE, true, false);
    }

    @Bean
    public Queue stockReserveQueue() {
        return QueueBuilder.durable(STOCK_RESERVE_QUEUE).build();
    }

    @Bean
    public Queue stockReleaseQueue() {
        return QueueBuilder.durable(STOCK_RELEASE_QUEUE).build();
    }

    @Bean
    public Queue stockConfirmQueue() {
        return QueueBuilder.durable(STOCK_CONFIRM_QUEUE).build();
    }

    @Bean
    public Binding stockReserveBinding() {
        return BindingBuilder.bind(stockReserveQueue()).to(orderExchange()).with("order.reserve");
    }

    @Bean
    public Binding stockReleaseBinding() {
        return BindingBuilder.bind(stockReleaseQueue()).to(orderExchange()).with("order.release");
    }

    @Bean
    public Binding stockConfirmBinding() {
        return BindingBuilder.bind(stockConfirmQueue()).to(orderExchange()).with("order.confirm");
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
