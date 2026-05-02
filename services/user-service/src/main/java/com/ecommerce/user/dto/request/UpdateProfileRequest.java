package com.ecommerce.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @NotBlank(message = "Ad boş olamaz")
    @Size(min = 2, max = 50)
    private String firstName;

    @NotBlank(message = "Soyad boş olamaz")
    @Size(min = 2, max = 50)
    private String lastName;

    @Pattern(regexp = "^(\\+90|0)?5\\d{9}$", message = "Geçerli bir Türkiye telefon numarası girin")
    private String phone;
}
