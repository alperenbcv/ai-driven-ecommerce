package com.ecommerce.product.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReviewResponse {
    private Long id;
    private Long userId;
    private String userName;
    private int rating;
    private String title;
    private String body;
    private boolean verifiedPurchase;
    private int helpfulCount;
    private LocalDateTime createdAt;
}
