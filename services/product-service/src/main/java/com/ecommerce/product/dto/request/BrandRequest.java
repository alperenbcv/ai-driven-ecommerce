package com.ecommerce.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BrandRequest {

    @NotBlank(message = "Marka adı boş olamaz")
    @Size(max = 100)
    private String name;

    private String logoUrl;
}
