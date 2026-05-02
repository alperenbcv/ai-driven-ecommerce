package com.ecommerce.order.entity;

public enum OrderStatus {
    PENDING,
    STOCK_RESERVED,
    PAYMENT_PROCESSING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUNDED
}
