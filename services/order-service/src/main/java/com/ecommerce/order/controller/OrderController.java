package com.ecommerce.order.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.order.dto.request.CreateOrderRequest;
import com.ecommerce.order.dto.response.OrderResponse;
import com.ecommerce.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Sipariş yönetimi")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Sipariş oluştur (Saga başlar)")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateOrderRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        orderService.createOrder(userId, request),
                        "Siparişiniz alındı. Stok ve ödeme işlemleri devam ediyor."));
    }

    @GetMapping("/{orderNumber}")
    @Operation(summary = "Sipariş detayı")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @PathVariable String orderNumber,
            @RequestHeader("X-User-Id") Long userId) {

        return ResponseEntity.ok(ApiResponse.success(
                orderService.getOrder(orderNumber, userId)));
    }

    @GetMapping
    @Operation(summary = "Siparişlerim")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getMyOrders(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(ApiResponse.success(
                orderService.getUserOrders(userId, page, size)));
    }
    @DeleteMapping("/{orderNumber}")
    @Operation(summary = "Sipariş iptal et")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @PathVariable String orderNumber,
            @RequestHeader("X-User-Id") Long userId) {

        orderService.cancelOrder(orderNumber, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Sipariş iptal edildi"));
    }
}
