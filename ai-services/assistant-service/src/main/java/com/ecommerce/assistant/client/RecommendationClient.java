package com.ecommerce.assistant.client;

import com.ecommerce.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
        name = "recommendation-service",
        path = "/api/recommendations"
)
public interface RecommendationClient {

    @GetMapping("/users/{userId}")
    ApiResponse<List<Long>> getForUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "6") int limit
    );

    @GetMapping("/products/{productId}")
    ApiResponse<List<Long>> getForProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "6") int limit
    );

    @GetMapping("/popular")
    ApiResponse<List<Long>> getPopular(
            @RequestParam(defaultValue = "6") int limit
    );
}
