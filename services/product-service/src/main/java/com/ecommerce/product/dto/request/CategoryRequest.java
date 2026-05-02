package com.ecommerce.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank(message = "Kategori adı boş olamaz")
    @Size(max = 100)
    private String name;

    @Size(max = 255)
    private String description;

    private String imageUrl;

    /** Null ise root kategori, değilse sub-kategori */
    private Long parentId;
}
