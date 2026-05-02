package com.ecommerce.search.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Semantik arama endpoint'i.
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Validated
@Tag(name = "Semantic Search", description = "Yapay zeka destekli anlamsal ürün arama")
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    @Operation(summary = "Semantik ürün arama — doğal dil destekli")
    public ResponseEntity<ApiResponse<List<Long>>> search(
            @RequestParam @NotBlank @Size(max = 300) String q,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int topK,
            @RequestParam(defaultValue = "0.6")
            @DecimalMin(value = "0.0", inclusive = true)
            @DecimalMax(value = "1.0", inclusive = true) double minScore) {

        List<Long> productIds = searchService.search(q, topK, minScore);
        return ResponseEntity.ok(ApiResponse.success(productIds));
    }
}
