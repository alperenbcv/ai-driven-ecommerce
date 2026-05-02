package com.ecommerce.product.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.product.dto.request.ProductProposalRequest;
import com.ecommerce.product.dto.request.ProposalReviewRequest;
import com.ecommerce.product.dto.response.ProductProposalResponse;
import com.ecommerce.product.service.ProductProposalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/proposals")
@RequiredArgsConstructor
@Tag(name = "Product Proposals", description = "Seller ürün teklifleri ve admin moderasyonu")
public class ProductProposalController {

    private final ProductProposalService proposalService;


    @PostMapping
    @Operation(summary = "Yeni ürün teklifi gönder (Seller)")
    public ResponseEntity<ApiResponse<ProductProposalResponse>> submitProposal(
            @RequestHeader("X-User-Id") Long sellerId,
            @RequestHeader("X-User-Role") String userRole,
            @Valid @RequestBody ProductProposalRequest request) {

        if (!"SELLER".equals(userRole) && !"ADMIN".equals(userRole)) {
            throw new BusinessException("Bu işlem için seller yetkisi gereklidir");
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        proposalService.submitProposal(sellerId, request),
                        "Teklifiniz incelemeye alındı"));
    }

    @GetMapping("/my")
    @Operation(summary = "Kendi tekliflerim (Seller)")
    public ResponseEntity<ApiResponse<PageResponse<ProductProposalResponse>>> getMyProposals(
            @RequestHeader("X-User-Id") Long sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(ApiResponse.success(
                proposalService.getSellerProposals(sellerId, page, size)));
    }


    @GetMapping("/pending")
    @Operation(summary = "Bekleyen teklifler (Admin)")
    public ResponseEntity<ApiResponse<PageResponse<ProductProposalResponse>>> getPendingProposals(
            @RequestHeader("X-User-Role") String userRole,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (!"ADMIN".equals(userRole)) {
            throw new BusinessException("Bu işlem için admin yetkisi gereklidir");
        }

        return ResponseEntity.ok(ApiResponse.success(
                proposalService.getPendingProposals(page, size)));
    }


    @PutMapping("/{proposalId}")
    @Operation(summary = "Teklifi revize et ve yeniden gönder (Seller)")
    public ResponseEntity<ApiResponse<ProductProposalResponse>> updateProposal(
            @PathVariable Long proposalId,
            @RequestHeader("X-User-Id") Long sellerId,
            @RequestHeader("X-User-Role") String userRole,
            @Valid @RequestBody ProductProposalRequest request) {

        if (!"SELLER".equals(userRole) && !"ADMIN".equals(userRole)) {
            throw new BusinessException("Bu işlem için seller yetkisi gereklidir");
        }

        return ResponseEntity.ok(ApiResponse.success(
                proposalService.updateProposal(proposalId, sellerId, request),
                "Teklifiniz revize edilerek yeniden incelemeye alındı"));
    }
    
    @PatchMapping("/{proposalId}/review")
    @Operation(summary = "Teklifi değerlendir (Admin)")
    public ResponseEntity<ApiResponse<ProductProposalResponse>> reviewProposal(
            @PathVariable Long proposalId,
            @RequestHeader("X-User-Role") String userRole,
            @Valid @RequestBody ProposalReviewRequest request) {

        if (!"ADMIN".equals(userRole)) {
            throw new BusinessException("Bu işlem için admin yetkisi gereklidir");
        }

        return ResponseEntity.ok(ApiResponse.success(
                proposalService.reviewProposal(proposalId, request),
                "Teklif değerlendirmesi tamamlandı"));
    }
}
