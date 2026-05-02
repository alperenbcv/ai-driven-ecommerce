package com.ecommerce.cart.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CartItem implements Serializable {

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
    private int quantity;

    @JsonIgnore
    public BigDecimal getTotalPrice() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
