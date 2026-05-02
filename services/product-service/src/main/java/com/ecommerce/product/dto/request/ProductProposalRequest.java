package com.ecommerce.product.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductProposalRequest {

    @NotBlank(message = "Ürün adı boş olamaz")
    @Size(max = 200)
    private String proposedName;

    @Size(max = 5000)
    private String proposedDescription;

    @NotNull(message = "Fiyat zorunlu")
    @DecimalMin(value = "0.01")
    private BigDecimal proposedPrice;

    @NotNull(message = "Kategori zorunlu")
    private Long categoryId;

    private Long brandId;
}
