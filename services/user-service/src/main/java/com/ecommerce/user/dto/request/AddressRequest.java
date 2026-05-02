package com.ecommerce.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AddressRequest {

    @NotBlank(message = "Adres başlığı boş olamaz")
    private String title;

    @NotBlank(message = "Ad boş olamaz")
    private String firstName;

    @NotBlank(message = "Soyad boş olamaz")
    private String lastName;

    @NotBlank(message = "Telefon numarası boş olamaz")
    @Pattern(regexp = "^(\\+90|0)?5\\d{9}$", message = "Geçerli bir Türkiye telefon numarası girin")
    private String phone;

    @NotBlank(message = "Şehir boş olamaz")
    private String city;

    @NotBlank(message = "İlçe boş olamaz")
    private String district;

    @NotBlank(message = "Adres boş olamaz")
    private String fullAddress;

    private boolean defaultAddress = false;
}
