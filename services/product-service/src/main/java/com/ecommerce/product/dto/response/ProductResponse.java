package com.ecommerce.product.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private boolean active;
    private CategorySummary category;
    private BrandSummary brand;
    private List<ProductImageResponse> images;
    private LocalDateTime createdAt;

    @Data
    public static class CategorySummary {
        private Long id;
        private String name;
    }

    @Data
    public static class BrandSummary {
        private Long id;
        private String name;
    }
}
