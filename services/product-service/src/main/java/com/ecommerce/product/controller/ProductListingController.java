package com.ecommerce.product.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.product.dto.request.ProductListingRequest;
import com.ecommerce.product.dto.response.ProductListingResponse;
import com.ecommerce.product.service.ProductListingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequiredArgsConstructor
@Tag(name = "Listings", description = "Seller ürün listeleri")
public class ProductListingController {

    private final ProductListingService listingService;


    @PostMapping("/api/products/{productId}/listings")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    @Operation(summary = "Katalog ürününe listing ekle (Seller)")
    public ResponseEntity<ApiResponse<ProductListingResponse>> createListing(
            @PathVariable Long productId,
            @RequestHeader("X-User-Id") Long sellerId,
            @Valid @RequestBody ProductListingRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        listingService.createListing(productId, sellerId, request),
                        "Listeniz oluşturuldu"));
    }

    @PatchMapping("/api/products/{productId}/listings")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    @Operation(summary = "Listing fiyatını güncelle (Seller)")
    public ResponseEntity<ApiResponse<ProductListingResponse>> updatePrice(
            @PathVariable Long productId,
            @RequestHeader("X-User-Id") Long sellerId,
            @Valid @RequestBody ProductListingRequest request) {

        return ResponseEntity.ok(ApiResponse.success(
                listingService.updateListingPrice(productId, sellerId, request)));
    }

    @DeleteMapping("/api/products/{productId}/listings")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    @Operation(summary = "Listingi devre dışı bırak (Seller)")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @PathVariable Long productId,
            @RequestHeader("X-User-Id") Long sellerId) {

        listingService.deactivateListing(productId, sellerId);
        return ResponseEntity.ok(ApiResponse.success(null, "Liste devre dışı bırakıldı"));
    }

    @PostMapping("/api/products/{productId}/listings/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    @Operation(summary = "Listingi aktif et (Seller)")
    public ResponseEntity<ApiResponse<Void>> activate(
            @PathVariable Long productId,
            @RequestHeader("X-User-Id") Long sellerId) {

        listingService.activateListing(productId, sellerId);
        return ResponseEntity.ok(ApiResponse.success(null, "Liste aktif edildi"));
    }

    @GetMapping("/api/products/{productId}/listings")
    @Operation(summary = "Ürünün tüm satıcı listelerini getir")
    public ResponseEntity<ApiResponse<List<ProductListingResponse>>> getListings(
            @PathVariable Long productId) {

        return ResponseEntity.ok(ApiResponse.success(
                listingService.getListingsForProduct(productId)));
    }

    @GetMapping("/api/listings/my")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    @Operation(summary = "Kendi listing'lerim (Seller)")
    public ResponseEntity<ApiResponse<List<ProductListingResponse>>> getMyListings(
            @RequestHeader("X-User-Id") Long sellerId) {

        return ResponseEntity.ok(ApiResponse.success(
                listingService.getSellerListings(sellerId)));
    }
}
