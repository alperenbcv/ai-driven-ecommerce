package com.ecommerce.product.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ProductListingResponse {
    private Long id;
    private Long productId;
    private String productName;
    private Long sellerId;
    private String sellerName;
    private BigDecimal price;
    private boolean active;
    private LocalDateTime createdAt;
}
