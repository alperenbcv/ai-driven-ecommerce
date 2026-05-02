package com.ecommerce.cargo.listener;

import com.ecommerce.cargo.service.CargoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Order Service'ten gelen "kargo oluştur" eventini dinler.
 *
 * Saga akışı:
 *   Order (PAID) → payment.success → order.cargo.create
 *   → CargoEventListener.onCreateCargo()
 *   → CargoServiceImpl.createShipment()
 *   → cargo.created event → Order (SHIPPED)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CargoEventListener {

    private final CargoService cargoService;

    @RabbitListener(queues = "cargo.create.queue")
    public void onCreateCargo(Map<String, Object> event) {
        log.info("Kargo oluşturma isteği alındı: {}", event.get("orderId"));
        cargoService.createShipment(event);
    }
}
