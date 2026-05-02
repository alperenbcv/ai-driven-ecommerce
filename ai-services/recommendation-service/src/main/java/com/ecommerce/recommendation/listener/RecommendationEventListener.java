package com.ecommerce.recommendation.listener;

import com.ecommerce.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Order Service'ten "sipariş tamamlandı" eventini dinler.
 * Graf güncellenir: (User)-[:PURCHASED]->(Product)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RecommendationEventListener {

    private final RecommendationService recommendationService;

    @RabbitListener(queues = "recommendation.order.queue")
    public void onOrderDelivered(Map<String, Object> event) {
        log.info("Sipariş tamamlandı eventi alındı: orderId={}", event.get("orderId"));
        try {
            recommendationService.recordPurchase(event);
        } catch (Exception e) {
            log.error("Öneri graafı güncellenemedi: {}", e.getMessage());
        }
    }
}
