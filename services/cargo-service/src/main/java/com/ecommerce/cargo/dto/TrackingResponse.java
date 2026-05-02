package com.ecommerce.cargo.dto;

import com.ecommerce.cargo.entity.Shipment.CargoStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TrackingResponse {
    private String trackingNumber;
    private String orderNumber;
    private String provider;
    private CargoStatus currentStatus;
    private String recipientName;
    private String city;
    private LocalDateTime estimatedDelivery;
    private LocalDateTime deliveredAt;
    private List<EventItem> events;

    @Data
    @Builder
    public static class EventItem {
        private CargoStatus status;
        private String description;
        private String location;
        private LocalDateTime timestamp;
    }
}
