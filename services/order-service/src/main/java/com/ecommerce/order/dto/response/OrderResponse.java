package com.ecommerce.order.dto.response;

import com.ecommerce.order.entity.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponse {
    private Long id;
    private String orderNumber;
    private Long userId;
    private OrderStatus status;
    private BigDecimal totalAmount;

    private String shippingFullName;
    private String shippingPhone;
    private String shippingCity;
    private String shippingDistrict;
    private String shippingFullAddress;

    private String cargoTrackingNumber;
    private String cargoProvider;
    private String cancelReason;

    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime shippedAt;

    @Data
    @Builder
    public static class OrderItemResponse {
        private Long id;
        private Long productId;
        private String productName;
        private BigDecimal unitPrice;
        private int quantity;
        private BigDecimal totalPrice;
        private Long sellerId;
    }
}
