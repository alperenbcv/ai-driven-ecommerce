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

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CargoServiceImpl implements CargoService {

    private final ShipmentRepository shipmentRepository;
    private final RabbitTemplate rabbitTemplate;
    private final DbBackedTrackingNumberGenerator trackingNumberGenerator;

    /**
     * Order Service'ten "cargo.create" eventi gelince yeni kargo oluştur.
     *
     * Mock provider: "MockCargo" — gerçek entegrasyonda Yurtiçi/Aras/MNG API'sine istek gider.
     * estimatedDelivery: 3 iş günü (mock)
     *
     * Kargo oluşturulunca "cargo.created" eventi yayınlanır → Order Service SHIPPED yapar.
     */
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

    /**
     * Mock kargo durum simülasyonu — Scheduler tarafından çağrılır.
     *
     * Her çağrıda aktif kargoları bir sonraki duruma ilerletir.
     * Gerçek kargo firması bu adımları saat/gün aralıklarla yapar.
     * Demo için dakika bazlı çalışır (scheduler cron'u yml'de ayarlanır).
     *
     * Durum akışı:
     *   CREATED → PICKED_UP → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED
     */
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
                // Order Service'e bildir → SHIPPED → DELIVERED + notification + recommendation
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
