package com.ecommerce.stock.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class StockCreateRequest {

    @NotNull(message = "Ürün ID zorunlu")
    private Long productId;

    private Long sellerId;

    @Min(value = 0, message = "Başlangıç stoku negatif olamaz")
    private int initialQuantity;

    @Min(value = 1, message = "Düşük stok eşiği en az 1 olmalı")
    private int lowStockThreshold = 5;
}
