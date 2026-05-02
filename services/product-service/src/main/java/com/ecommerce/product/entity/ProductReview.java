package com.ecommerce.product.entity;

import com.ecommerce.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 *
 * userId / userName user adını değiştirirse eski yorumları etkilemesin diye tutuluyor.
 *
 * verifiedPurchase alanı bu kullanıcı bu ürünü gerçekten satın aldı mı diye kontrol amaçlı tutuluyor.
 * Order Service'e async sorgu yapılır. İlk oluşturulduğunda false,
 * sipariş doğrulanırsa güncelleme eventi gelir.
 *
 * @Table(uniqueConstraints) amacı aynı kullanıcı aynı ürüne sadece bir yorum yapabilir.
 */
@Entity
@Table(
    name = "product_reviews",
    indexes = {
        @Index(name = "idx_review_product", columnList = "product_id"),
        @Index(name = "idx_review_user", columnList = "user_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_review_user_product",
            columnNames = {"user_id", "product_id"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductReview extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_name", nullable = false, length = 100)
    private String userName;

    @Column(nullable = false)
    private int rating;

    @Column(length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false)
    @Builder.Default
    private boolean verifiedPurchase = false;

    @Column(nullable = false)
    @Builder.Default
    private int helpfulCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean approved = true;
}
