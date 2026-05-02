package com.ecommerce.product.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductRequest {

    @NotBlank(message = "Ürün adı boş olamaz")
    @Size(max = 200, message = "Ürün adı en fazla 200 karakter olabilir")
    private String name;

    @Size(max = 5000, message = "Açıklama en fazla 5000 karakter olabilir")
    private String description;

    @NotNull(message = "Fiyat boş olamaz")
    @DecimalMin(value = "0.01", message = "Fiyat 0'dan büyük olmalı")
    @Digits(integer = 8, fraction = 2, message = "Fiyat formatı hatalı")
    private BigDecimal price;

    @NotNull(message = "Kategori zorunlu")
    private Long categoryId;

    private Long brandId;
}
