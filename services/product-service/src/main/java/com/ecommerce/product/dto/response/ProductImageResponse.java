package com.ecommerce.product.dto.response;

import lombok.Data;

@Data
public class ProductImageResponse {
    private Long id;
    private String url;
    private int displayOrder;
}
