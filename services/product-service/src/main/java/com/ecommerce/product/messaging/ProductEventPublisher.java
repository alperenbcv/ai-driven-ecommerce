package com.ecommerce.product.messaging;

import com.ecommerce.product.config.RabbitMQConfig;
import com.ecommerce.product.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Search-service ile ortak sözleşme: routing key + payload alanları burada tek kaynak.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishCreated(Product product) {
        publishUpsert(product, RabbitMQConfig.ROUTING_PRODUCT_CREATED, "product.created");
    }

    public void publishUpdated(Product product) {
        publishUpsert(product, RabbitMQConfig.ROUTING_PRODUCT_UPDATED, "product.updated");
    }

    private void publishUpsert(Product product, String routingKey, String eventType) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.PRODUCT_EXCHANGE,
                    routingKey,
                    buildUpsertPayload(product, eventType));
            log.debug("Product upsert event: routingKey={}, productId={}", routingKey, product.getId());
        } catch (Exception e) {
            log.warn("RabbitMQ event yayınlanamadı (ürün kaydedildi): {}", e.getMessage());
        }
    }

    public void publishDeleted(Long productId) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "product.deleted");
            event.put("productId", productId);
            event.put("active", false);
            event.put("occurredAt", Instant.now().toString());
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.PRODUCT_EXCHANGE,
                    RabbitMQConfig.ROUTING_PRODUCT_DELETED,
                    event);
            log.debug("Product deleted event: productId={}", productId);
        } catch (Exception e) {
            log.warn("RabbitMQ delete event yayınlanamadı: {}", e.getMessage());
        }
    }

    private static Map<String, Object> buildUpsertPayload(Product product, String eventType) {
        String categoryName = product.getCategory() != null ? product.getCategory().getName() : "";
        String brandName = product.getBrand() != null ? product.getBrand().getName() : "";
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("productId", product.getId());
        event.put("name", product.getName());
        event.put("description", product.getDescription() != null ? product.getDescription() : "");
        event.put("category", categoryName);
        event.put("categoryName", categoryName);
        event.put("brand", brandName);
        event.put("price", product.getPrice());
        event.put("active", product.isActive());
        event.put("occurredAt", Instant.now().toString());
        if (product.getSellerId() != null) {
            event.put("sellerId", product.getSellerId());
        }
        return event;
    }
}
