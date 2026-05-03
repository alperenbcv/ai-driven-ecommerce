package com.ecommerce.cargo.service;

import com.ecommerce.cargo.config.RabbitMQConfig;
import com.ecommerce.cargo.dto.TrackingResponse;
import com.ecommerce.cargo.entity.Shipment;
import com.ecommerce.cargo.entity.Shipment.CargoStatus;
import com.ecommerce.cargo.entity.ShipmentEvent;
import com.ecommerce.cargo.repository.ShipmentRepository;
import com.ecommerce.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Cargo Service'in ana iş mantığı implementasyonudur.
 *
 * Bu servis, sipariş servisinden gelen kargo oluşturma event'ini işler,
 * kargo takip kaydı oluşturur, takip numarası üretir ve kargo durumlarını
 * simüle ederek ilerletir.
 *
 * Temel görevleri:
 * 1. createShipment:
 *    Order Service'ten gelen event içindeki sipariş ve alıcı bilgilerini alır.
 *    Aynı sipariş için daha önce kargo oluşturulmuşsa tekrar kayıt açmaz.
 *    Yeni bir trackingNumber üretir, Shipment kaydı oluşturur ve ilk
 *    ShipmentEvent olarak CREATED durumunu ekler.
 *
 * 2. advanceShipmentStatuses:
 *    Teslim edilmemiş veya başarısız olmamış aktif kargoları bulur.
 *    Her çağrıda kargo durumunu bir sonraki adıma taşır.
 *
 * 3. track:
 *    Takip numarasına göre kargo bilgisini döndürür.
 *
 * 4. trackByOrder:
 *    Sipariş numarasına göre ilgili kargo bilgisini döndürür.
 *
 * RabbitMQ kullanımı:
 * - Kargo oluşturulduğunda cargo.created eventi yayınlanır.
 * - Kargo DELIVERED durumuna geldiğinde cargo.delivered eventi yayınlanır.
 *   Böylece Order Service gibi diğer servisler kargo durumundan haberdar olabilir.
 *
 * @Transactional(readOnly = true):
 * Sınıf seviyesinde varsayılan olarak sadece okuma transaction'ı kullanılır.
 * Veri değiştiren createShipment ve advanceShipmentStatuses metotlarında ayrıca
 * @Transactional kullanılarak yazma işlemleri aktif hale getirilir.
 *
 * Yardımcı metotlar:
 * - getNextStatus bir sonraki kargo durumunu belirler.
 * - getStatusDescription kullanıcıya gösterilecek açıklamayı üretir.
 * - getStatusLocation durum bazlı mock lokasyon bilgisini üretir.
 * - toResponse Shipment entity'sini frontend'e dönecek TrackingResponse DTO'suna çevirir.
 *
 */

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CargoServiceImpl implements CargoService {

    private final ShipmentRepository shipmentRepository;
    private final RabbitTemplate rabbitTemplate;
    private final DbBackedTrackingNumberGenerator trackingNumberGenerator;

    @Transactional
    @Override
    public void createShipment(Map<String, Object> event) {
        String orderNumber = event.get("orderId").toString();

        if (shipmentRepository.findByOrderNumber(orderNumber).isPresent()) {
            log.warn("Bu sipariş için kargo zaten oluşturulmuş: {}", orderNumber);
            return;
        }

        String trackingNumber = trackingNumberGenerator.nextMockCargoNumber(LocalDate.now());

        Shipment shipment = Shipment.builder()
                .orderNumber(orderNumber)
                .trackingNumber(trackingNumber)
                .provider("MockCargo")
                .recipientName(event.getOrDefault("recipientName", "").toString())
                .recipientPhone(event.getOrDefault("recipientPhone", "").toString())
                .address(event.getOrDefault("address", "").toString())
                .city(event.getOrDefault("city", "").toString())
                .district(event.getOrDefault("district", "").toString())
                .estimatedDelivery(LocalDateTime.now().plusDays(3))
                .build();

        ShipmentEvent createdEvent = ShipmentEvent.builder()
                .status(CargoStatus.CREATED)
                .description("Kargolamanız hazırlandı. Kurye bekliyor.")
                .location("Depo")
                .build();
        shipment.addEvent(createdEvent);

        shipmentRepository.save(shipment);
        log.info("Kargo oluşturuldu: orderNumber={}, tracking={}", orderNumber, trackingNumber);

        publishCargoCreatedEvent(orderNumber, trackingNumber);
    }

    @Transactional
    @Override
    public void advanceShipmentStatuses() {
        List<Shipment> activeShipments = shipmentRepository.findByStatusNotIn(
                List.of(CargoStatus.DELIVERED, CargoStatus.FAILED));

        for (Shipment shipment : activeShipments) {
            CargoStatus nextStatus = getNextStatus(shipment.getStatus());
            if (nextStatus == null) continue;

            shipment.setStatus(nextStatus);

            ShipmentEvent event = ShipmentEvent.builder()
                    .status(nextStatus)
                    .description(getStatusDescription(nextStatus))
                    .location(getStatusLocation(nextStatus, shipment.getCity()))
                    .build();
            shipment.addEvent(event);

            if (nextStatus == CargoStatus.DELIVERED) {
                shipment.setDeliveredAt(LocalDateTime.now());
                publishCargoDeliveredEvent(shipment.getOrderNumber());
            }

            shipmentRepository.save(shipment);
            log.info("Kargo durumu güncellendi: tracking={}, yeniDurum={}", shipment.getTrackingNumber(), nextStatus);
        }
    }

    @Override
    public TrackingResponse track(String trackingNumber) {
        Shipment shipment = shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new NotFoundException("Takip numarası bulunamadı: " + trackingNumber));
        return toResponse(shipment);
    }

    @Override
    public TrackingResponse trackByOrder(String orderNumber) {
        Shipment shipment = shipmentRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new NotFoundException("Siparişe ait kargo bulunamadı: " + orderNumber));
        return toResponse(shipment);
    }

    private void publishCargoCreatedEvent(String orderNumber, String trackingNumber) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CARGO_EXCHANGE,
                RabbitMQConfig.CARGO_CREATED_KEY,
                Map.of(
                        "orderId", orderNumber,
                        "trackingNumber", trackingNumber,
                        "provider", "MockCargo"));
    }

    private void publishCargoDeliveredEvent(String orderNumber) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CARGO_EXCHANGE,
                RabbitMQConfig.CARGO_DELIVERED_KEY,
                Map.of("orderId", orderNumber));
        log.info("cargo.delivered eventi yayınlandı: orderNumber={}", orderNumber);
    }

    private CargoStatus getNextStatus(CargoStatus current) {
        return switch (current) {
            case CREATED -> CargoStatus.PICKED_UP;
            case PICKED_UP -> CargoStatus.IN_TRANSIT;
            case IN_TRANSIT -> CargoStatus.OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> CargoStatus.DELIVERED;
            default -> null;
        };
    }

    private String getStatusDescription(CargoStatus status) {
        return switch (status) {
            case PICKED_UP -> "Kurye gönderinizi adresten teslim aldı.";
            case IN_TRANSIT -> "Gönderiniz dağıtım merkezine ulaştı.";
            case OUT_FOR_DELIVERY -> "Gönderiniz dağıtım aracında, bugün teslim edilecek.";
            case DELIVERED -> "Gönderiniz teslim edildi.";
            default -> "Durum güncellendi.";
        };
    }

    private String getStatusLocation(CargoStatus status, String city) {
        return switch (status) {
            case PICKED_UP -> city + " - Alım Noktası";
            case IN_TRANSIT -> city + " - Dağıtım Merkezi";
            case OUT_FOR_DELIVERY -> city + " - Dağıtım Aracı";
            case DELIVERED -> city + " - Teslim Noktası";
            default -> city;
        };
    }

    private TrackingResponse toResponse(Shipment shipment) {
        List<TrackingResponse.EventItem> events = shipment.getEvents().stream()
                .map(e -> TrackingResponse.EventItem.builder()
                        .status(e.getStatus())
                        .description(e.getDescription())
                        .location(e.getLocation())
                        .timestamp(e.getCreatedAt())
                        .build())
                .toList();

        return TrackingResponse.builder()
                .trackingNumber(shipment.getTrackingNumber())
                .orderNumber(shipment.getOrderNumber())
                .provider(shipment.getProvider())
                .currentStatus(shipment.getStatus())
                .recipientName(shipment.getRecipientName())
                .city(shipment.getCity())
                .estimatedDelivery(shipment.getEstimatedDelivery())
                .deliveredAt(shipment.getDeliveredAt())
                .events(events)
                .build();
    }
}
