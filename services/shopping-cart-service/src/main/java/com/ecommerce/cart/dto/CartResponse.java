package com.ecommerce.cart.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CartResponse {
    private Long userId;
    private List<CartItem> items;
    private int totalItems;
    private int totalQuantity;
    private BigDecimal totalAmount;
}
