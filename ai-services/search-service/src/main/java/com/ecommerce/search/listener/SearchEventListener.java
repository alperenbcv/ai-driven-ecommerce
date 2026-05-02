package com.ecommerce.search.listener;

import com.ecommerce.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Product Service'ten gelen ürün olayları.
 * Index kuyruğu yalnızca {@code product.created} / {@code product.updated} routing key'leriyle dolar;
 * silme yalnız delete kuyruğundadır.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SearchEventListener {

    private final SearchService searchService;

    @RabbitListener(queues = "search.product.index.queue")
    public void onProductCreatedOrUpdated(Map<String, Object> event) {
        String type = event.getOrDefault("eventType", "UNKNOWN").toString();
        if ("product.deleted".equals(type)) {
            log.warn("Index kuyruğunda silme eventi yok sayıldı (beklenmeyen): productId={}", event.get("productId"));
            return;
        }
        log.info("Ürün index eventi alındı: type={}, productId={}", type, event.get("productId"));
        try {
            searchService.indexProduct(event);
        } catch (Exception e) {
            log.error("Ürün katalog satırı işlenemedi: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "search.product.delete.queue")
    public void onProductDeleted(Map<String, Object> event) {
        Long productId = Long.valueOf(event.get("productId").toString());
        log.info("Ürün index'ten çıkarılıyor: {}", productId);
        try {
            searchService.deactivateProduct(productId);
        } catch (Exception e) {
            log.error("Ürün index'ten çıkarılamadı: {}", e.getMessage(), e);
        }
    }
}
