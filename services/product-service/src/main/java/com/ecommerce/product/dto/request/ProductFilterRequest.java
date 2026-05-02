package com.ecommerce.product.dto.request;

import lombok.Data;

import java.math.BigDecimal;


@Data
public class ProductFilterRequest {

    private String keyword;
    private Long categoryId;
    private Long brandId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    // Sayfalama
    private int page = 0;
    private int size = 20;

    // Sıralama: "name", "price", "createdAt"
    private String sortBy = "createdAt";
    // "asc" veya "desc"
    private String sortDir = "desc";
}
