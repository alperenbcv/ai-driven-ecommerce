package com.ecommerce.stock.listener;

import com.ecommerce.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockEventListener {

    private final StockService stockService;

    @RabbitListener(queues = "stock.reserve.queue")
    public void onReserveStock(Map<String, Object> event) {
        log.info("Stok rezervasyon isteği alındı: {}", event);
        String orderId = event.get("orderId").toString();
        Long productId = Long.valueOf(event.get("productId").toString());
        int amount = Integer.parseInt(event.get("amount").toString());
        Long sellerId = event.containsKey("sellerId") && event.get("sellerId") != null
                ? Long.valueOf(event.get("sellerId").toString()) : null;

        stockService.reserveStock(productId, sellerId, amount, orderId);
    }

    @RabbitListener(queues = "stock.release.queue")
    public void onReleaseStock(Map<String, Object> event) {
        log.info("Stok iade isteği alındı: {}", event);
        String orderId = event.get("orderId").toString();
        Long productId = Long.valueOf(event.get("productId").toString());
        int amount = Integer.parseInt(event.get("amount").toString());
        Long sellerId = event.containsKey("sellerId") && event.get("sellerId") != null
                ? Long.valueOf(event.get("sellerId").toString()) : null;

        stockService.releaseStock(productId, sellerId, amount, orderId);
    }

    @RabbitListener(queues = "stock.confirm.queue")
    public void onConfirmStock(Map<String, Object> event) {
        log.info("Stok onay isteği alındı: {}", event);
        String orderId = event.get("orderId").toString();
        Long productId = Long.valueOf(event.get("productId").toString());
        int amount = Integer.parseInt(event.get("amount").toString());
        Long sellerId = event.containsKey("sellerId") && event.get("sellerId") != null
                ? Long.valueOf(event.get("sellerId").toString()) : null;

        stockService.confirmStock(productId, sellerId, amount, orderId);
    }
}
