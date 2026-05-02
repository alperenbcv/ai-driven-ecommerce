package com.ecommerce.order.entity;

import com.ecommerce.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * Adres snapshot kısmı ÖNEMLİ:
 * Kullanıcı siparişten sonra adresini değiştirebilir.
 * Sipariş, verildiği andaki adres bilgisini tutmalı.
 * Bu yüzden adres alanları doğrudan Order tablosuna kopyalanır.
 * User Service'e JOIN atmak yerine her şey burada.
 */
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_user", columnList = "user_id"),
    @Index(name = "idx_order_number", columnList = "order_number", unique = true),
    @Index(name = "idx_order_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {

    @Column(name = "order_number", nullable = false, unique = true, length = 30)
    private String orderNumber;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    // Bu da aslnda kullanıcının mail'inin snapshot'u
    @Column(name = "user_email", length = 150)
    private String userEmail;

    @Column(nullable = false, length = 150)
    private String shippingFullName;

    @Column(nullable = false, length = 20)
    private String shippingPhone;

    @Column(nullable = false, length = 100)
    private String shippingCity;

    @Column(nullable = false, length = 100)
    private String shippingDistrict;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String shippingFullAddress;

    @Column(length = 100)
    private String paymentIntentId;

    @Column(length = 50)
    private String cargoTrackingNumber;

    @Column(length = 50)
    private String cargoProvider;

    @Column
    private LocalDateTime shippedAt;

    @Column
    private LocalDateTime deliveredAt;

    @Column(length = 500)
    private String cancelReason;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public boolean isCancellable() {
        return status == OrderStatus.PENDING
                || status == OrderStatus.STOCK_RESERVED
                || status == OrderStatus.PAYMENT_PROCESSING;
    }
}
