package com.ecommerce.payment.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.payment.entity.PaymentTransaction;
import com.ecommerce.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "Iyzico ödeme entegrasyonu")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/callback")
    @Operation(summary = "Iyzico ödeme callback'i (Iyzico tarafından çağrılır)")
    public ResponseEntity<String> handleCallback(
            @RequestParam("token") String token,
            @RequestParam(value = "status", defaultValue = "") String status) {

        log.info("Iyzico callback alındı: token={}, status={}", token, status);
        paymentService.handleCallback(token, status);

        return ResponseEntity.ok("OK");
    }

    @PostMapping("/orders/{orderNumber}/simulate-success")
    @Operation(summary = "Sandbox: ödemeyi başarılı simüle et")
    public ResponseEntity<ApiResponse<String>> simulateSuccess(
            @PathVariable String orderNumber) {

        log.warn("[SANDBOX] Ödeme simüle ediliyor: {}", orderNumber);
        paymentService.simulateSuccess(orderNumber);
        return ResponseEntity.ok(ApiResponse.success("Ödeme simüle edildi", "SUCCESS"));
    }

    @GetMapping("/orders/{orderNumber}")
    @Operation(summary = "Sipariş ödeme durumu (+ Iyzico form içeriği)")
    public ResponseEntity<ApiResponse<PaymentSummary>> getPaymentStatus(
            @PathVariable String orderNumber) {

        try {
            PaymentTransaction tx = paymentService.getByOrderNumber(orderNumber);
            return ResponseEntity.ok(ApiResponse.success(new PaymentSummary(
                    tx.getOrderNumber(),
                    tx.getStatus().name(),
                    tx.getAmount(),
                    tx.getIyzicoPaymentId(),
                    tx.getFailureReason(),
                    tx.getRawResponse()
            )));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.success(new PaymentSummary(
                    orderNumber, "PENDING", null, null, null, null
            )));
        }
    }

    record PaymentSummary(
            String orderNumber,
            String status,
            java.math.BigDecimal amount,
            String paymentId,
            String failureReason,
            String checkoutFormContent
    ) {}
}
