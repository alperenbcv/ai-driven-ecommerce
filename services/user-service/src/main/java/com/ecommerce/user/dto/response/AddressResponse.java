package com.ecommerce.user.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddressResponse {

    private Long id;
    private String title;
    private String firstName;
    private String lastName;
    private String phone;
    private String city;
    private String district;
    private String fullAddress;
    private boolean defaultAddress;
}
