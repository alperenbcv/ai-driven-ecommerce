package com.ecommerce.cargo.controller;

import com.ecommerce.cargo.dto.TrackingResponse;
import com.ecommerce.cargo.service.CargoService;
import com.ecommerce.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Kargo takip endpoint'leri.
 *
 * Takip numarasıyla sorgulama herkesin yapabileceği bir işlem.
 * JWT gerekmez — bu endpoint API Gateway'de public.
 *
 * Sipariş numarasıyla sorgulama iç kullanım (Order Service veya auth'd kullanıcı).
 */
@RestController
@RequestMapping("/api/cargo")
@RequiredArgsConstructor
@Tag(name = "Cargo", description = "Kargo takip")
public class CargoController {

    private final CargoService cargoService;

    @GetMapping("/track/{trackingNumber}")
    @Operation(summary = "Takip numarasıyla kargo sorgula")
    public ResponseEntity<ApiResponse<TrackingResponse>> track(
            @PathVariable String trackingNumber) {
        return ResponseEntity.ok(ApiResponse.success(cargoService.track(trackingNumber)));
    }

    @GetMapping("/orders/{orderNumber}")
    @Operation(summary = "Sipariş numarasıyla kargo sorgula")
    public ResponseEntity<ApiResponse<TrackingResponse>> trackByOrder(
            @PathVariable String orderNumber) {
        return ResponseEntity.ok(ApiResponse.success(cargoService.trackByOrder(orderNumber)));
    }
}
