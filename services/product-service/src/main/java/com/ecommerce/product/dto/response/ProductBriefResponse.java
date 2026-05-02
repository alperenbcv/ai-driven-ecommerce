package com.ecommerce.product.dto.response;

import com.ecommerce.product.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductBriefResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String categoryName;
    private String brandName;
    private BigDecimal averageRating;
    private Integer reviewCount;
    private Boolean active;

    public static ProductBriefResponse from(Product p) {
        return ProductBriefResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .brandName(p.getBrand() != null ? p.getBrand().getName() : null)
                .averageRating(p.getAverageRating())
                .reviewCount(p.getReviewCount())
                .active(p.isActive())
                .build();
    }
}
