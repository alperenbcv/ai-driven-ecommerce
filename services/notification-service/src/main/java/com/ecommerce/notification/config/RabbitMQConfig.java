package com.ecommerce.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Notification Service RabbitMQ konfigürasyonu.
 *
 * Notification Service sadece DİNLER, event YAYINLAMAz.
 * Diğer servislerden gelen eventlere göre e-posta gönderir.
 *
 * DİNLENEN EVENTLER:
 *   order.exchange → order.created   → "Siparişiniz alındı" maili
 *   order.exchange → order.shipped   → "Kargolandı" maili + tracking no
 *   order.exchange → order.delivered.notification → "Teslim edildi" maili
 *   order.exchange → order.cancelled → "Sipariş iptal edildi" maili
 *   payment.exchange → payment.failed → "Ödeme başarısız" maili
 *   stock.exchange → stock.low       → Admin'e düşük stok uyarısı
 */
@Configuration
public class RabbitMQConfig {

    @Bean TopicExchange orderExchange()   { return new TopicExchange("order.exchange",   true, false); }
    @Bean TopicExchange paymentExchange() { return new TopicExchange("payment.exchange", true, false); }
    @Bean TopicExchange stockExchange()   { return new TopicExchange("stock.exchange",   true, false); }
    @Bean TopicExchange userExchange()    { return new TopicExchange("user.exchange",    true, false); }

    @Bean
    public Queue notifyOrderCreatedQueue()    { return QueueBuilder.durable("notify.order.created.queue").build(); }
    @Bean
    public Queue notifyOrderShippedQueue()    { return QueueBuilder.durable("notify.order.shipped.queue").build(); }
    @Bean
    public Queue notifyOrderDeliveredQueue()  { return QueueBuilder.durable("notify.order.delivered.queue").build(); }
    @Bean
    public Queue notifyOrderCancelledQueue()  { return QueueBuilder.durable("notify.order.cancelled.queue").build(); }
    @Bean
    public Queue notifyPaymentFailedQueue()   { return QueueBuilder.durable("notify.payment.failed.queue").build(); }
    @Bean
    public Queue notifyLowStockQueue()        { return QueueBuilder.durable("notify.stock.low.queue").build(); }
    @Bean
    public Queue notifyUserRegisteredQueue()  { return QueueBuilder.durable("notify.user.registered.queue").build(); }
    @Bean
    public Queue notifyUserPasswordResetQueue() { return QueueBuilder.durable("notify.user.password-reset.queue").build(); }

    @Bean Binding bindOrderCreated()   { return BindingBuilder.bind(notifyOrderCreatedQueue()).to(orderExchange()).with("order.created"); }
    @Bean Binding bindOrderShipped()   { return BindingBuilder.bind(notifyOrderShippedQueue()).to(orderExchange()).with("order.shipped"); }
    @Bean Binding bindOrderDelivered() { return BindingBuilder.bind(notifyOrderDeliveredQueue()).to(orderExchange()).with("order.delivered.notification"); }
    @Bean Binding bindOrderCancelled() { return BindingBuilder.bind(notifyOrderCancelledQueue()).to(orderExchange()).with("order.cancelled"); }
    @Bean Binding bindPaymentFailed()  { return BindingBuilder.bind(notifyPaymentFailedQueue()).to(paymentExchange()).with("payment.failed"); }
    @Bean Binding bindLowStock()       { return BindingBuilder.bind(notifyLowStockQueue()).to(stockExchange()).with("stock.low"); }
    @Bean Binding bindUserRegistered() { return BindingBuilder.bind(notifyUserRegisteredQueue()).to(userExchange()).with("user.registered"); }
    @Bean Binding bindUserPasswordReset() { return BindingBuilder.bind(notifyUserPasswordResetQueue()).to(userExchange()).with("user.password-reset"); }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() { return new Jackson2JsonMessageConverter(); }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(messageConverter());
        return t;
    }
}
