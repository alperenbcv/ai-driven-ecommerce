package com.ecommerce.user.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.user.dto.request.LoginRequest;
import com.ecommerce.user.dto.request.RegisterRequest;
import com.ecommerce.user.dto.response.AuthResponse;
import com.ecommerce.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Kayıt, giriş ve şifre yönetimi")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Yeni kullanıcı kaydı")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Kayıt başarılı. Lütfen e-postanızı doğrulayın."));
    }

    @PostMapping("/login")
    @Operation(summary = "Kullanıcı girişi")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Giriş başarılı"));
    }

    @GetMapping("/verify-email")
    @Operation(summary = "E-posta doğrulama linki")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success(null, "E-posta adresiniz başarıyla doğrulandı"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Şifremi unuttum — sıfırlama maili gönder")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("E-posta adresi gerekli"));
        }
        authService.forgotPassword(email);
        return ResponseEntity.ok(ApiResponse.success(null,
            "Eğer bu e-posta sistemimizde kayıtlıysa sıfırlama linki gönderildi"));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Yeni şifre belirle")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.success(null, "Şifreniz başarıyla güncellendi"));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Access token yenile (refresh token ile)")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("refreshToken gerekli"));
        }
        AuthResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(response, "Token yenilendi"));
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Doğrulama mailini yeniden gönder")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("E-posta adresi gerekli"));
        }
        authService.resendVerificationEmail(email);
        return ResponseEntity.ok(ApiResponse.success(null, "Doğrulama maili gönderildi"));
    }

    @GetMapping("/check-email")
    @Operation(summary = "E-posta kullanımda mı? (DB doğrulamalı)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkEmail(@RequestParam String email) {
        boolean exists = authService.checkEmailExists(email);
        return ResponseEntity.ok(ApiResponse.success(Map.of("exists", exists)));
    }

    record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 6, message = "Şifre en az 6 karakter olmalı") String newPassword
    ) {}
}
