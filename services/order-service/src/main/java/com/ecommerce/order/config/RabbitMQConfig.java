package com.ecommerce.order.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Order Service RabbitMQ konfigürasyonu.
 *
 * Order Service hem yayınlar hem dinler:
 *
 * ── YAYINLAR (Giden) ──────────────────────────────────────────────────────
 * order.exchange → order.reserve   : Stock Service'e stok rezerve et
 * order.exchange → order.release   : Stock Service'e stoku iade et
 * order.exchange → order.confirm   : Stock Service'e stoku kalıcı düşür
 * order.exchange → payment.initiate: Payment Service'e ödemeyi başlat
 * order.exchange → cargo.create    : Cargo Service'e kargo oluştur
 * order.exchange → order.delivered.notification / order.delivered.recommendation : teslim sonrası bildirim ve öneri
 *
 * ── DİNLER (Gelen) ────────────────────────────────────────────────────────
 * stock.exchange → stock.reserved  : Stok başarıyla rezerve edildi
 * stock.exchange → stock.failed    : Stok rezervasyonu başarısız
 * payment.exchange → payment.success : Ödeme başarılı
 * payment.exchange → payment.failed  : Ödeme başarısız
 * cargo.exchange → cargo.created   : Kargo oluşturuldu (takip numarası geldi)
 *
 * Her exchange ve queue durable (restart'ta kaybolmaz).
 * TopicExchange → wildcard routing key desteği.
 */
@Configuration
public class RabbitMQConfig {

    // ─── Giden Exchangeler ────────────────────────────────────────────────────
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String ORDER_RESERVE_KEY = "order.reserve";
    public static final String ORDER_RELEASE_KEY = "order.release";
    public static final String ORDER_CONFIRM_KEY = "order.confirm";
    public static final String PAYMENT_INITIATE_KEY = "payment.initiate";
    public static final String CARGO_CREATE_KEY = "cargo.create";

    // Notification / Recommendation servislerinin dinleyeceği lifecycle eventleri
    public static final String ORDER_CREATED_KEY = "order.created";
    public static final String ORDER_SHIPPED_KEY = "order.shipped";
    public static final String ORDER_CANCELLED_KEY = "order.cancelled";
    /** Notification için teslim bildirimi (userEmail snapshot payload). */
    public static final String ORDER_DELIVERED_NOTIFICATION_KEY = "order.delivered.notification";
    /** Recommendation graf güncellemesi (userId/items payload). */
    public static final String ORDER_DELIVERED_RECOMMENDATION_KEY = "order.delivered.recommendation";

    // ─── Gelen Queue'lar ──────────────────────────────────────────────────────
    public static final String ORDER_STOCK_RESERVED_QUEUE = "order.stock.reserved.queue";
    public static final String ORDER_STOCK_FAILED_QUEUE = "order.stock.failed.queue";
    public static final String ORDER_PAYMENT_SUCCESS_QUEUE = "order.payment.success.queue";
    public static final String ORDER_PAYMENT_FAILED_QUEUE = "order.payment.failed.queue";
    public static final String ORDER_CARGO_CREATED_QUEUE = "order.cargo.created.queue";
    public static final String ORDER_CARGO_DELIVERED_QUEUE = "order.cargo.delivered.queue";

    // ─── Exchange'ler ─────────────────────────────────────────────────────────
    @Bean TopicExchange orderExchange() { return new TopicExchange(ORDER_EXCHANGE, true, false); }
    @Bean TopicExchange stockExchange() { return new TopicExchange("stock.exchange", true, false); }
    @Bean TopicExchange paymentExchange() { return new TopicExchange("payment.exchange", true, false); }
    @Bean TopicExchange cargoExchange() { return new TopicExchange("cargo.exchange", true, false); }

    // ─── Gelen Queue'lar ──────────────────────────────────────────────────────
    @Bean Queue orderStockReservedQueue() { return QueueBuilder.durable(ORDER_STOCK_RESERVED_QUEUE).build(); }
    @Bean Queue orderStockFailedQueue() { return QueueBuilder.durable(ORDER_STOCK_FAILED_QUEUE).build(); }
    @Bean Queue orderPaymentSuccessQueue() { return QueueBuilder.durable(ORDER_PAYMENT_SUCCESS_QUEUE).build(); }
    @Bean Queue orderPaymentFailedQueue() { return QueueBuilder.durable(ORDER_PAYMENT_FAILED_QUEUE).build(); }
    @Bean Queue orderCargoCreatedQueue() { return QueueBuilder.durable(ORDER_CARGO_CREATED_QUEUE).build(); }
    @Bean Queue orderCargoDeliveredQueue() { return QueueBuilder.durable(ORDER_CARGO_DELIVERED_QUEUE).build(); }

    // ─── Binding'ler ──────────────────────────────────────────────────────────
    @Bean Binding stockReservedBinding() {
        return BindingBuilder.bind(orderStockReservedQueue()).to(stockExchange()).with("stock.reserved");
    }
    @Bean Binding stockFailedBinding() {
        return BindingBuilder.bind(orderStockFailedQueue()).to(stockExchange()).with("stock.failed");
    }
    @Bean Binding paymentSuccessBinding() {
        return BindingBuilder.bind(orderPaymentSuccessQueue()).to(paymentExchange()).with("payment.success");
    }
    @Bean Binding paymentFailedBinding() {
        return BindingBuilder.bind(orderPaymentFailedQueue()).to(paymentExchange()).with("payment.failed");
    }
    @Bean Binding cargoCreatedBinding() {
        return BindingBuilder.bind(orderCargoCreatedQueue()).to(cargoExchange()).with("cargo.created");
    }
    @Bean Binding cargoDeliveredBinding() {
        return BindingBuilder.bind(orderCargoDeliveredQueue()).to(cargoExchange()).with("cargo.delivered");
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
