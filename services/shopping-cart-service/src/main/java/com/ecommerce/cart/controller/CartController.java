package com.ecommerce.cart.controller;

import com.ecommerce.cart.dto.AddToCartRequest;
import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.service.CartServiceImpl;
import com.ecommerce.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Alışveriş sepeti yönetimi")
public class CartController {

    private final CartServiceImpl cartService;

    @GetMapping
    @Operation(summary = "Sepeti görüntüle")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(ApiResponse.success(cartService.getCart(userId)));
    }

    @PostMapping("/items")
    @Operation(summary = "Sepete ürün ekle")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody AddToCartRequest request) {
        return ResponseEntity.ok(ApiResponse.success(cartService.addItem(userId, request)));
    }

    @PatchMapping("/items/{productId}/{listingId}")
    @Operation(summary = "Sepetteki ürün adedini güncelle (0 = sil)")
    public ResponseEntity<ApiResponse<CartResponse>> updateQuantity(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long productId,
            @PathVariable Long listingId,
            @RequestParam int quantity) {
        return ResponseEntity.ok(ApiResponse.success(
                cartService.updateItemQuantity(userId, productId, listingId, quantity)));
    }

    @DeleteMapping("/items/{productId}/{listingId}")
    @Operation(summary = "Sepetten ürün çıkar")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long productId,
            @PathVariable Long listingId) {
        return ResponseEntity.ok(ApiResponse.success(
                cartService.removeItem(userId, productId, listingId)));
    }

    @DeleteMapping
    @Operation(summary = "Sepeti tamamen temizle")
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @RequestHeader("X-User-Id") Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
