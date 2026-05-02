package com.ecommerce.assistant.controller;

import com.ecommerce.assistant.dto.ChatRequest;
import com.ecommerce.assistant.dto.ChatResponse;
import com.ecommerce.assistant.dto.ProductDescriptionRequest;
import com.ecommerce.assistant.dto.ProductDescriptionResponse;
import com.ecommerce.assistant.service.AssistantService;
import com.ecommerce.assistant.service.ProductImageGenerationService;
import com.ecommerce.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI Assistant", description = "Yapay zeka destekli müşteri asistanı")
public class AssistantController {

    private final AssistantService assistantService;
    private final ProductImageGenerationService productImageGenerationService;

    @PostMapping("/chat")
    @Operation(summary = "AI asistanla sohbet et.")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(ApiResponse.success(assistantService.chat(request, userId)));
    }

    @DeleteMapping("/chat/{sessionId}")
    @Operation(summary = "Konuşma geçmişini temizler.")
    public ResponseEntity<ApiResponse<Void>> clearSession(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable String sessionId) {
        assistantService.clearSession(sessionId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/product-description")
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    @Operation(summary = "ADMIN / SELLER için AI ürün açıklaması üretir.")
    public ResponseEntity<ApiResponse<ProductDescriptionResponse>> generateProductDescription(
            @Valid @RequestBody ProductDescriptionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(assistantService.generateProductDescription(request)));
    }

    @PostMapping("/generate-product-image")
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    @Operation(
            summary = "DALLE 2 ile ürün görseli URL'si üret")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> generateProductImage(
            @RequestBody java.util.Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(productImageGenerationService.generateProductImage(body)));
    }
}
