package com.ecommerce.cart.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AddToCartRequest {

    @NotNull
    private Long productId;

    @NotNull
    private Long listingId;

    @NotNull
    private Long sellerId;

    @NotBlank
    private String productName;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal unitPrice;

    @Min(1)
    private int quantity = 1;
}
