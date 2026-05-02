package com.ecommerce.stock.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StockResponse {
    private Long id;
    private Long productId;
    private Long sellerId;
    private int quantity;
    private int reservedQty;
    private int availableQty;
    private int lowStockThreshold;
    private boolean lowStock;
}
