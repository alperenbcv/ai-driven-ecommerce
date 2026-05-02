package com.ecommerce.order.listener;

import com.ecommerce.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaEventListener {

    private final OrderService orderService;

    @RabbitListener(queues = "order.stock.reserved.queue")
    public void onStockReserved(Map<String, Object> event) {
        String orderNumber = requiredString(event, "orderId");
        Long productId = requiredLong(event, "productId");
        log.info("Saga: stok rezerve edildi → {}", orderNumber);
        orderService.onStockReserved(orderNumber, productId);
    }

    @RabbitListener(queues = "order.stock.failed.queue")
    public void onStockFailed(Map<String, Object> event) {
        String orderNumber = requiredString(event, "orderId");
        String reason = stringOrDefault(event, "reason", "Yetersiz stok");
        log.info("Saga: stok başarısız → {}", orderNumber);
        orderService.onStockFailed(orderNumber, reason);
    }

    @RabbitListener(queues = "order.payment.success.queue")
    public void onPaymentSuccess(Map<String, Object> event) {
        String orderNumber = requiredString(event, "orderId");
        String paymentIntentId = stringOrDefault(event, "paymentIntentId", "");
        log.info("Saga: ödeme başarılı → {}", orderNumber);
        orderService.onPaymentSuccess(orderNumber, paymentIntentId);
    }

    @RabbitListener(queues = "order.payment.failed.queue")
    public void onPaymentFailed(Map<String, Object> event) {
        String orderNumber = requiredString(event, "orderId");
        String reason = stringOrDefault(event, "reason", "Ödeme başarısız");
        log.info("Saga: ödeme başarısız → {}", orderNumber);
        orderService.onPaymentFailed(orderNumber, reason);
    }

    @RabbitListener(queues = "order.cargo.created.queue")
    public void onCargoCreated(Map<String, Object> event) {
        String orderNumber = requiredString(event, "orderId");
        String trackingNumber = requiredString(event, "trackingNumber");
        String provider = stringOrDefault(event, "provider", "MockCargo");
        log.info("Saga: kargo oluşturuldu → {}, tracking={}", orderNumber, trackingNumber);
        orderService.onCargoCreated(orderNumber, trackingNumber, provider);
    }

    @RabbitListener(queues = "order.cargo.delivered.queue")
    public void onCargoDelivered(Map<String, Object> event) {
        String orderNumber = requiredString(event, "orderId");
        log.info("Saga: kargo teslim edildi → {}", orderNumber);
        orderService.onCargoDelivered(orderNumber);
    }

    private static String requiredString(Map<String, Object> event, String key) {
        Object v = event.get(key);
        if (v == null || v.toString().isBlank()) {
            throw new IllegalArgumentException("Saga event eksik zorunlu alan: " + key + ", payload=" + event.keySet());
        }
        return v.toString();
    }

    private static String stringOrDefault(Map<String, Object> event, String key, String defaultVal) {
        Object v = event.get(key);
        return v != null && !v.toString().isBlank() ? v.toString() : defaultVal;
    }

    private static Long requiredLong(Map<String, Object> event, String key) {
        Object v = event.get(key);
        if (v == null) {
            throw new IllegalArgumentException("Saga event eksik zorunlu alan: " + key + ", payload=" + event.keySet());
        }
        try {
            if (v instanceof Number n) {
                return n.longValue();
            }
            return Long.valueOf(v.toString());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Saga event geçersiz sayı alanı: " + key + "=" + v, ex);
        }
    }
}
