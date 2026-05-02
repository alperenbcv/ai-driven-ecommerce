package com.ecommerce.assistant.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProductDescriptionResponse {

    private String description;
    private String seoTitle;
    private List<String> tags;
}
