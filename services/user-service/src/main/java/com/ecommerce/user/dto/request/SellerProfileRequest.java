package com.ecommerce.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SellerProfileRequest {

    @NotBlank(message = "Mağaza adı zorunlu")
    @Size(max = 100, message = "Mağaza adı en fazla 100 karakter")
    private String storeName;

    @Size(max = 500, message = "Mağaza açıklaması en fazla 500 karakter")
    private String storeDescription;
}
