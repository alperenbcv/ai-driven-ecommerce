package com.ecommerce.product.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductListingRequest {

    @NotNull(message = "Fiyat zorunlu")
    @DecimalMin(value = "0.01", message = "Fiyat 0'dan büyük olmalı")
    @Digits(integer = 8, fraction = 2, message = "Fiyat formatı hatalı")
    private BigDecimal price;
}
