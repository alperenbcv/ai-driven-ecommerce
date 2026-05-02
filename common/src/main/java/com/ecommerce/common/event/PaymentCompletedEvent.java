package com.ecommerce.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent {

    private UUID orderId;
    private Long userId;
    private String paymentId;
    private BigDecimal amount;
    private boolean success;
    private String failureReason;
    private String correlationId;
}
