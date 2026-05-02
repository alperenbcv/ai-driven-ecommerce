package com.ecommerce.stock.entity;

import com.ecommerce.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 *
 * quantity       : mevcut satılabilir stok
 * reservedQty    : sipariş verilmiş ama henüz ödenmemiş rezervasyonlar
 * availableQty   : quantity - reservedQty = gerçekten satılabilir olan
 *
 * Neden ikisi ayrı?
 * "Stok: 10, Rezerve: 3" durumunda kullanıcıya 7 gösterilir.
 * Ama veritabanında 10 var — ödeme başarısız olursa 3 geri gelecek.
 *
 * lowStockThreshold : bu değerin altına düşünce admin'e uyarı gönderilir.
 *
 * @Version : Optimistic locking için yedek plan.
 * Biz atomic UPDATE kullanıyoruz ama ek güvenlik katmanı olarak tutuyoruz.
 */
@Entity
@Table(name = "stocks",
    uniqueConstraints = @UniqueConstraint(name = "uq_stock_product_seller", columnNames = {"product_id", "seller_id"}),
    indexes = @Index(name = "idx_stock_product", columnList = "product_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "seller_id")
    private Long sellerId;

    @Column(nullable = false)
    @Builder.Default
    private int quantity = 0;

    @Column(nullable = false)
    @Builder.Default
    private int reservedQty = 0;

    @Column(nullable = false)
    @Builder.Default
    private int lowStockThreshold = 5;

    @Version
    private Long version;

    public int getAvailableQty() {
        return quantity - reservedQty;
    }

    public boolean isLowStock() {
        return getAvailableQty() <= lowStockThreshold;
    }
}
