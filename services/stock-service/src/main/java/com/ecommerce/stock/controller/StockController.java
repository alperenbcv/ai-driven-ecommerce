package com.ecommerce.stock.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.stock.dto.request.StockAdjustRequest;
import com.ecommerce.stock.dto.request.StockCreateRequest;
import com.ecommerce.stock.dto.response.StockResponse;
import com.ecommerce.stock.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
@Tag(name = "Stock", description = "Stok yönetimi")
public class StockController {

    private final StockService stockService;

    @PostMapping
    @Operation(summary = "Stok kaydı oluştur/güncelle")
    public ResponseEntity<ApiResponse<StockResponse>> createStock(
            @Valid @RequestBody StockCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(stockService.createOrUpdateStock(request), "Stok oluşturuldu/güncellendi"));
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Ürün toplam stok bilgisi")
    public ResponseEntity<ApiResponse<StockResponse>> getStock(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(stockService.getStockByProductId(productId)));
    }

    @GetMapping("/product/{productId}/seller/{sellerId}")
    @Operation(summary = "Satıcıya özgü stok bilgisi")
    public ResponseEntity<ApiResponse<StockResponse>> getStockBySeller(
            @PathVariable Long productId,
            @PathVariable Long sellerId) {
        return ResponseEntity.ok(ApiResponse.success(
                stockService.getStockByProductIdAndSellerId(productId, sellerId)));
    }

    @GetMapping("/product/{productId}/all")
    @Operation(summary = "Ürünün tüm satıcı stoklarını getir")
    public ResponseEntity<ApiResponse<List<StockResponse>>> getAllStocks(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(stockService.getAllStocksForProduct(productId)));
    }

    @PatchMapping("/product/{productId}/adjust")
    @Operation(summary = "Stok düzelt (Admin/Seller)")
    public ResponseEntity<ApiResponse<StockResponse>> adjustStock(
            @PathVariable Long productId,
            @RequestHeader(value = "X-User-Id", required = false) Long headerSellerId,
            @RequestParam(required = false) Long sellerId,
            @Valid @RequestBody StockAdjustRequest request) {

        Long resolvedSellerId = headerSellerId != null ? headerSellerId : sellerId;
        return ResponseEntity.ok(ApiResponse.success(
                stockService.adjustStock(productId, resolvedSellerId, request), "Stok güncellendi"));
    }
}
