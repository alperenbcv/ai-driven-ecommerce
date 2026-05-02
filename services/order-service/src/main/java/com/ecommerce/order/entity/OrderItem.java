package com.ecommerce.order.entity;

import com.ecommerce.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 *
 * Neden productName, unitPrice burada tekrar var:
 * → Snapshot pattern: Ürün adı ve fiyatı ileride değişebilir.
 *   Sipariş verildiği andaki fiyat ve isim burada saklanır.
 *
 * listingId → Hangi seller'ın listing'inden alındığı.
 *   Birden fazla seller aynı ürünü satıyorsa kullanıcı hangi seller'dan aldı?
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "listing_id")
    private Long listingId;

    @Column(name = "seller_id")
    private Long sellerId;

    @Column(nullable = false, length = 200)
    private String productName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Column(nullable = false)
    @Builder.Default
    private boolean stockReserved = false;
}
