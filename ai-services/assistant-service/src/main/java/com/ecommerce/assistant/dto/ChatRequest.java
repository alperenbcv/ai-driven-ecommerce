package com.ecommerce.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatRequest {

    @NotBlank
    @Size(max = 2000)
    private String message;

    private String sessionId;
}
