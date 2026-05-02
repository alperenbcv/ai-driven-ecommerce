package com.ecommerce.assistant.client;

import com.ecommerce.assistant.dto.tool.ProductSummary;
import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.common.dto.PageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Product Service'e Feign Client.
 *
 * İstek adresi: `name` ile Eureka / LoadBalancer'dan çözülür (ayrı `url` yok).
 */
@FeignClient(name = "product-service", path = "/api/products")
public interface ProductClient {

    @GetMapping("/batch")
    ApiResponse<List<ProductSummary>> batchByIds(@RequestParam("ids") List<Long> ids);

    @GetMapping
    ApiResponse<PageResponse<ProductSummary>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    );

    @GetMapping("/{id}")
    ApiResponse<ProductSummary> getById(@PathVariable Long id);
}
