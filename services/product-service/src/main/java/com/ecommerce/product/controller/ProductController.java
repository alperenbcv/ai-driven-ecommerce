package com.ecommerce.product.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.product.dto.request.ProductFilterRequest;
import com.ecommerce.product.dto.request.ProductRequest;
import com.ecommerce.product.dto.response.ProductBriefResponse;
import com.ecommerce.product.dto.response.ProductResponse;
import com.ecommerce.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Ürün yönetimi")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Katalog ürünü oluştur (sadece Admin)")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(productService.createProduct(request), "Ürün oluşturuldu"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Katalog ürünü güncelle (sadece Admin)")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success(productService.updateProduct(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ürün sil (soft delete) — sadece Admin")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Ürün silindi"));
    }

    @GetMapping("/batch")
    @Operation(summary = "Aktif ürün özetleri — toplu (en fazla 80 id)")
    public ResponseEntity<ApiResponse<java.util.List<ProductBriefResponse>>> batchByIds(
            @RequestParam("ids") java.util.List<Long> ids) {
        return ResponseEntity.ok(ApiResponse.success(productService.batchActiveSummaries(ids)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ürün detayı")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductById(id)));
    }

    @GetMapping
    @Operation(summary = "Ürün listesi (filtreleme + sayfalama)")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> listProducts(
            @ModelAttribute ProductFilterRequest filter) {
        return ResponseEntity.ok(ApiResponse.success(productService.listProducts(filter)));
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    @Operation(summary = "Ürün görseli yükle (Admin veya ürün sahibi satıcı)")
    public ResponseEntity<ApiResponse<ProductResponse>> uploadImage(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "displayOrder", defaultValue = "0") int displayOrder) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(productService.uploadImage(id, file, displayOrder), "Görsel yüklendi"));
    }

    @DeleteMapping("/{productId}/images/{imageId}")
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    @Operation(summary = "Ürün görseli sil (Admin veya ürün sahibi satıcı)")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @PathVariable Long productId,
            @PathVariable Long imageId) {
        productService.deleteImage(productId, imageId);
        return ResponseEntity.ok(ApiResponse.success(null, "Görsel silindi"));
    }

    @PostMapping("/{id}/images/from-url")
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    @Operation(summary = "URL'den görsel yükle (Admin veya ürün sahibi satıcı; AI URL'leri için)")
    public ResponseEntity<ApiResponse<ProductResponse>> uploadImageFromUrl(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, Object> body) {
        String imageUrl    = (String) body.get("imageUrl");
        int    displayOrder = body.containsKey("displayOrder") ? (int) body.get("displayOrder") : 0;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(productService.uploadImageFromUrl(id, imageUrl, displayOrder), "Görsel yüklendi"));
    }

    @PatchMapping("/{productId}/images/{imageId}")
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    @Operation(summary = "Görsel sırasını güncelle (Admin veya ürün sahibi satıcı)")
    public ResponseEntity<ApiResponse<Void>> updateImageOrder(
            @PathVariable Long productId,
            @PathVariable Long imageId,
            @RequestParam int displayOrder) {
        productService.updateImageOrder(productId, imageId, displayOrder);
        return ResponseEntity.ok(ApiResponse.success(null, "Sıra güncellendi"));
    }
}
