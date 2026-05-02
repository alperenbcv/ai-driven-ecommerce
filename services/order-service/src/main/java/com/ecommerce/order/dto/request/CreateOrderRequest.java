package com.ecommerce.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequest {

    @Email
    @NotNull(message = "Kullanıcı e-postası zorunlu")
    private String userEmail;

    @NotEmpty(message = "Sipariş en az bir ürün içermelidir")
    @Valid
    private List<OrderItemRequest> items;

    @NotNull(message = "Teslimat adresi zorunlu")
    @Valid
    private ShippingAddressRequest shippingAddress;

    @Data
    public static class OrderItemRequest {
        @NotNull(message = "Ürün ID zorunlu")
        private Long productId;

        private Long listingId;

        @NotNull(message = "Miktar zorunlu")
        private Integer quantity;

        private java.math.BigDecimal unitPrice;

        private String productName;

        private Long sellerId;
    }

    @Data
    public static class ShippingAddressRequest {
        @NotNull private String fullName;
        @NotNull private String phone;
        @NotNull private String city;
        @NotNull private String district;
        @NotNull private String fullAddress;
    }
}
