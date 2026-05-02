package com.ecommerce.recommendation.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.recommendation.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Validated
@Tag(name = "Recommendations", description = "Graf tabanlı ürün öneri sistemi")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/users/{userId}")
    @Operation(summary = "Kullanıcıya özel ürün önerileri")
    public ResponseEntity<ApiResponse<List<Long>>> forUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return ResponseEntity.ok(ApiResponse.success(
                recommendationService.getRecommendationsForUser(userId, limit)));
    }

    @GetMapping("/products/{productId}")
    @Operation(summary = "Bu ürünü alanlar bunu da aldı")
    public ResponseEntity<ApiResponse<List<Long>>> forProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "6") @Min(1) @Max(50) int limit) {
        return ResponseEntity.ok(ApiResponse.success(
                recommendationService.getProductBasedRecommendations(productId, limit)));
    }

    @GetMapping("/popular")
    @Operation(summary = "Popüler ürün önerileri")
    public ResponseEntity<ApiResponse<List<Long>>> popular(
            @RequestParam(defaultValue = "6") @Min(1) @Max(50) int limit) {
        return ResponseEntity.ok(ApiResponse.success(
                recommendationService.getPopularProducts(limit)));
    }

    @PostMapping("/track/view")
    @Operation(summary = "Ürün görüntüleme olayını kaydet (Neo4j VIEWED ilişkisi)")
    public ResponseEntity<ApiResponse<Void>> trackView(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody TrackViewRequest request) {
        if (userId == null || request.productId() == null) {
            return ResponseEntity.ok(ApiResponse.success(null));
        }
        recommendationService.recordView(userId, request.productId(), request.productName(), request.category());
        return ResponseEntity.ok(ApiResponse.success(null, "Görüntüleme kaydedildi"));
    }

    record TrackViewRequest(Long productId, String productName, String category) {}
}
