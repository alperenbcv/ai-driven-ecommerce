package com.ecommerce.stock.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class StockAdjustRequest {

    @NotNull
    @Min(value = 0, message = "Stok miktarı negatif olamaz")
    private Integer quantity;

    @NotBlank(message = "Düzeltme nedeni zorunlu")
    private String reason;
}
