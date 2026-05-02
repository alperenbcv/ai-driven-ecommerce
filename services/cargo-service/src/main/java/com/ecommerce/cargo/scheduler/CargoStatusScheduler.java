package com.ecommerce.cargo.scheduler;

import com.ecommerce.cargo.service.CargoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Mock kargo durum simülatörü.
 *
 * @Scheduled → Spring'in zamanlayıcı anotasyonu.
 * @EnableScheduling ile aktif edilir (main class veya config).
 *
 * fixedDelay = 120000 → her 2 dakikada bir çalışır.
 * Demo için makul: kargo oluşturulunca ~8 dakika içinde "teslim" durumuna geçer.
 * (CREATED → PICKED_UP → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED = 4 adım × 2 dk)
 *
 * Production'da bu scheduler çalışmaz — gerçek kargo firmasının webhook'ları gelir:
 *   POST /api/cargo/webhook  body: { trackingNumber: "...", status: "DELIVERED" }
 * Biz bu webhook'u işleyip durumu güncellerdik.
 *
 * initialDelay = 30000 → Uygulama başlayınca 30 saniye bekle, sonra başla.
 * DB bağlantısı ve RabbitMQ hazır olsun diye.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CargoStatusScheduler {

    private final CargoService cargoService;

    @Scheduled(fixedDelay = 120000, initialDelay = 30000)
    public void simulateCargoProgress() {
        log.debug("Mock kargo durum simülasyonu çalışıyor...");
        cargoService.advanceShipmentStatuses();
    }
}
