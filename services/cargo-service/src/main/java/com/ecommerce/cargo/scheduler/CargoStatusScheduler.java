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
 * @EnableScheduling ile aktif edilir.
 *
 * fixedDelay = 120000 → her 2 dakikada bir çalışır. Demo için kargo status simülasyonu yapar.
 * 
 * Şuan demo dışında webhook elemesi vs. yok yani production ortamında kargo status'un güncellenmesi
 * için ekleme yapılması gerekiyor.
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
