package com.ecommerce.product.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.product.dto.request.ReviewRequest;
import com.ecommerce.product.dto.response.ReviewResponse;
import com.ecommerce.product.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Ürün yorumları")
public class ReviewController {

    private final ReviewService reviewService;


    @PostMapping
    @Operation(summary = "Ürüne yorum ekle (Giriş gerekli)")
    public ResponseEntity<ApiResponse<ReviewResponse>> addReview(
            @PathVariable Long productId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Name") String userName,
            @Valid @RequestBody ReviewRequest request) {

        ReviewResponse review = reviewService.addReview(productId, userId, userName, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(review, "Yorumunuz eklendi"));
    }

    @GetMapping
    @Operation(summary = "Ürün yorumlarını listele")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(ApiResponse.success(
                reviewService.getReviews(productId, page, size)));
    }

    @DeleteMapping("/{reviewId}")
    @Operation(summary = "Yorum sil (kendi yorumu veya Admin)")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable Long productId,
            @PathVariable Long reviewId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String userRole) {

        boolean isAdmin = "ADMIN".equals(userRole);
        reviewService.deleteReview(productId, reviewId, userId, isAdmin);
        return ResponseEntity.ok(ApiResponse.success(null, "Yorum silindi"));
    }

    @PostMapping("/{reviewId}/helpful")
    @Operation(summary = "Yorumu faydalı olarak işaretle")
    public ResponseEntity<ApiResponse<Void>> markHelpful(
            @PathVariable Long productId,
            @PathVariable Long reviewId) {

        reviewService.markHelpful(reviewId);
        return ResponseEntity.ok(ApiResponse.success(null, "Teşekkürler!"));
    }
}
