package com.ecommerce.stock.entity;

import com.ecommerce.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "stock_movements", indexes = {
    @Index(name = "idx_movement_product", columnList = "product_id"),
    @Index(name = "idx_movement_order", columnList = "reference_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement extends BaseEntity {

    public enum MovementType {
        STOCK_IN, RESERVE, RELEASE, CONFIRM, ADJUSTMENT, RETURN
    }

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovementType movementType;

    @Column(nullable = false)
    private int delta;

    @Column(name = "reference_id")
    private String referenceId;

    @Column(length = 500)
    private String note;

    @Column(nullable = false)
    private int quantityBefore;

    @Column(nullable = false)
    private int quantityAfter;
}
