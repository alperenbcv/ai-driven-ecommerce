package com.ecommerce.assistant.client;

import com.ecommerce.assistant.dto.tool.OrderSummary;
import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.common.dto.PageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Order Service'e Feign Client.
 *
 * Basitlik için header-based yaklaşım kullandım.
 * 
 * name eşleşmesi eureka tarafında yapılır.
 * bu nedenle url eklemedim.
 */

@FeignClient(
        name = "order-service",
        path = "/api/orders"
)
public interface OrderClient {

    @GetMapping("/{orderNumber}")
    ApiResponse<OrderSummary> getByOrderNumber(
            @PathVariable String orderNumber,
            @RequestHeader("X-User-Id") Long userId
    );

    @GetMapping
    ApiResponse<PageResponse<OrderSummary>> getMyOrders(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    );
}
