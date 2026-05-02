package com.ecommerce.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProductDescriptionRequest {

    @NotBlank(message = "Ürün adı zorunlu")
    private String productName;

    private String categoryName;
    private String brandName;
    private String currentDescription;
}
