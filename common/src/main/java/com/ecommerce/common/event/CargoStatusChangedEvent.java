package com.ecommerce.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CargoStatusChangedEvent {

    private UUID orderId;
    private String cargoCode;
    private String carrierName;
    private String status;
    private String userEmail;
    private LocalDateTime changedAt;
}
