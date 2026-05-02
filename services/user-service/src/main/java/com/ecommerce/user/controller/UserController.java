package com.ecommerce.user.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.user.dto.request.AddressRequest;
import com.ecommerce.user.dto.request.ChangePasswordRequest;
import com.ecommerce.user.dto.request.SellerProfileRequest;
import com.ecommerce.user.dto.request.UpdateProfileRequest;
import com.ecommerce.user.dto.response.AddressResponse;
import com.ecommerce.user.dto.response.UserResponse;
import com.ecommerce.user.entity.RoleName;
import com.ecommerce.user.security.AuthUserPrincipal;
import com.ecommerce.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Kullanıcı profil ve adres yönetimi")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;


    @GetMapping("/{userId}/public")
    @Operation(summary = "Herkese açık kullanıcı bilgisi")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getPublicUser(
            @PathVariable Long userId) {
        UserResponse user = userService.getCurrentUser(userId);
        String displayName = (user.getStoreName() != null && !user.getStoreName().isBlank())
                ? user.getStoreName()
                : user.getFirstName() + " " + user.getLastName();
        return ResponseEntity.ok(ApiResponse.success(java.util.Map.of(
                "id", user.getId(),
                "displayName", displayName,
                "storeName", user.getStoreName() != null ? user.getStoreName() : "",
                "storeDescription", user.getStoreDescription() != null ? user.getStoreDescription() : "",
                "role", user.getRole()
        )));
    }

    @GetMapping("/me")
    @Operation(summary = "Mevcut kullanıcı bilgileri")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal AuthUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(userService.getCurrentUser(principal.getId())));
    }

    @PutMapping("/me")
    @Operation(summary = "Profil güncelle (ad, soyad, telefon)")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.updateProfile(principal.getId(), request)));
    }

    @PutMapping("/me/store-profile")
    @Operation(summary = "Mağaza profilini güncelle (Seller)")
    public ResponseEntity<ApiResponse<UserResponse>> updateStoreProfile(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @Valid @RequestBody SellerProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.updateSellerProfile(principal.getId(), request),
                "Mağaza profili güncellendi"));
    }

    @PatchMapping("/me/password")
    @Operation(summary = "Şifre değiştir")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(null, "Şifre başarıyla değiştirildi"));
    }

    @GetMapping("/me/addresses")
    @Operation(summary = "Kullanıcının adres listesi")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(
            @AuthenticationPrincipal AuthUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(userService.getAddresses(principal.getId())));
    }

    @PostMapping("/me/addresses")
    @Operation(summary = "Yeni adres ekle")
    public ResponseEntity<ApiResponse<AddressResponse>> addAddress(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse address = userService.addAddress(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(address, "Adres eklendi"));
    }

    @PutMapping("/me/addresses/{addressId}")
    @Operation(summary = "Adres güncelle")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long addressId,
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.updateAddress(principal.getId(), addressId, request)));
    }

    @DeleteMapping("/me/addresses/{addressId}")
    @Operation(summary = "Adres sil")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long addressId) {
        userService.deleteAddress(principal.getId(), addressId);
        return ResponseEntity.ok(ApiResponse.success(null, "Adres silindi"));
    }

    @PatchMapping("/me/addresses/{addressId}/default")
    @Operation(summary = "Varsayılan adres olarak ayarla")
    public ResponseEntity<ApiResponse<Void>> setDefaultAddress(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long addressId) {
        userService.setDefaultAddress(principal.getId(), addressId);
        return ResponseEntity.ok(ApiResponse.success(null, "Varsayılan adres güncellendi"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tüm kullanıcıları listele (Admin)")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(userService.getAllUsers(page, size)));
    }

    @PutMapping("/{targetUserId}/store-profile")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Satıcı mağaza profilini güncelle (Admin)")
    public ResponseEntity<ApiResponse<UserResponse>> updateStoreProfileByAdmin(
            @PathVariable Long targetUserId,
            @Valid @RequestBody SellerProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.updateSellerProfileByAdmin(targetUserId, request),
                "Mağaza profili güncellendi"));
    }

    @PatchMapping("/{targetUserId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Kullanıcı rolünü değiştir (sadece Admin)")
    public ResponseEntity<ApiResponse<UserResponse>> changeUserRole(
            @PathVariable Long targetUserId,
            @RequestParam RoleName role) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.changeUserRole(targetUserId, role),
                "Kullanıcı rolü güncellendi: " + role));
    }
}
