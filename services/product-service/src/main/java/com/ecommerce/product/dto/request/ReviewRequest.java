package com.ecommerce.product.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ReviewRequest {

    @Min(value = 1, message = "Puan en az 1 olmalı")
    @Max(value = 5, message = "Puan en fazla 5 olabilir")
    private int rating;

    @Size(max = 200, message = "Başlık en fazla 200 karakter olabilir")
    private String title;

    @NotBlank(message = "Yorum içeriği boş olamaz")
    @Size(min = 10, max = 2000, message = "Yorum 10-2000 karakter arasında olmalı")
    private String body;
}
