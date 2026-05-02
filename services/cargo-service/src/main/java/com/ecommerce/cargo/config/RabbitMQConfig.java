package com.ecommerce.cargo.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cargo Service için RabbitMQ konfigürasyonu.
 *
 * Bu sınıf, kargo oluşturma sürecinde kullanılan exchange, queue,
 * binding ve mesaj dönüştürücü bean'lerini tanımlar.
 *
 * Akış:
 * 1. Order Service, sipariş kargoya hazır olduğunda "order.exchange"
 *    üzerine "cargo.create" routing key'i ile event yayınlar.
 *
 * 2. Cargo Service, "cargo.create.queue" kuyruğunu dinler.
 *    Bu kuyruk "order.exchange" üzerindeki "cargo.create" eventlerine bind edilmiştir.
 *
 * 3. Cargo Service event'i işleyerek kargo kaydı, takip numarası ve
 *    ilk takip hareketlerini oluşturur.
 *
 * 4. Kargo oluşturulduktan veya teslim edildikten sonra Cargo Service,
 *    "cargo.exchange" üzerinden dışarıya event yayınlayabilir:
 *      - cargo.created   → kargo oluşturuldu
 *      - cargo.delivered → kargo teslim edildi
 *
 * TopicExchange:
 * Routing key'e göre mesaj yönlendirmesi yapar.
 * durable=true olduğu için RabbitMQ yeniden başlasa bile exchange korunur.
 *
 * QueueBuilder.durable(...):
 * Kuyruğun kalıcı olmasını sağlar. Böylece servis veya RabbitMQ yeniden başlasa bile
 * kuyruk kaybolmaz.
 *
 * Binding:
 * "cargo.create.queue" kuyruğunu "order.exchange" üzerindeki "cargo.create"
 * routing key'ine bağlar. Bu sayede sipariş servisinden gelen kargo oluşturma
 * mesajları Cargo Service tarafından alınabilir.
 *
 * Jackson2JsonMessageConverter:
 * RabbitMQ mesajlarının Java object / Map yapıları ile JSON arasında otomatik
 * dönüştürülmesini sağlar.
 *
 * RabbitTemplate:
 * Cargo Service'in RabbitMQ'ya mesaj yayınlamak için kullandığı ana yardımcı sınıftır.
 * Örneğin kargo oluşturulduğunda veya teslim edildiğinde event publish etmek için kullanılır.
 */

@Configuration
public class RabbitMQConfig {

    public static final String CARGO_EXCHANGE = "cargo.exchange";
    public static final String CARGO_CREATED_KEY = "cargo.created";
    public static final String CARGO_DELIVERED_KEY = "cargo.delivered";

    public static final String CARGO_CREATE_QUEUE = "cargo.create.queue";

    @Bean TopicExchange orderExchange() { return new TopicExchange("order.exchange", true, false); }
    @Bean TopicExchange cargoExchange() { return new TopicExchange(CARGO_EXCHANGE, true, false); }

    @Bean
    public Queue cargoCreateQueue() {
        return QueueBuilder.durable(CARGO_CREATE_QUEUE).build();
    }

    @Bean
    public Binding cargoCreateBinding() {
        return BindingBuilder.bind(cargoCreateQueue()).to(orderExchange()).with("cargo.create");
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
