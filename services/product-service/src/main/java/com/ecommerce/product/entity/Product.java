package com.ecommerce.product.entity;

import com.ecommerce.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_product_category", columnList = "category_id"),
    @Index(name = "idx_product_brand", columnList = "brand_id"),
    @Index(name = "idx_product_active", columnList = "active"),
    @Index(name = "idx_product_seller", columnList = "seller_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "seller_id")
    private Long sellerId;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean embeddingGenerated = false;

    @Column(nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private int reviewCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductReview> reviews = new ArrayList<>();

    /**
     * Yeni yorum eklenince ortalama rating'i güncelle.
     */
    public void addReviewRating(int newRating) {
        BigDecimal total = averageRating.multiply(BigDecimal.valueOf(reviewCount))
                .add(BigDecimal.valueOf(newRating));
        reviewCount++;
        averageRating = total.divide(BigDecimal.valueOf(reviewCount), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Yorum silinince ortalama rating'i güncelle.
     * Eğer son yorum silindiyse sıfırla.
     */
    public void removeReviewRating(int removedRating) {
        if (reviewCount <= 1) {
            reviewCount = 0;
            averageRating = BigDecimal.ZERO;
            return;
        }
        BigDecimal total = averageRating.multiply(BigDecimal.valueOf(reviewCount))
                .subtract(BigDecimal.valueOf(removedRating));
        reviewCount--;
        averageRating = total.divide(BigDecimal.valueOf(reviewCount), 2, java.math.RoundingMode.HALF_UP);
    }

    public void addImage(ProductImage image) {
        images.add(image);
        image.setProduct(this);
    }

    public void removeImage(ProductImage image) {
        images.remove(image);
        image.setProduct(null);
    }
}
