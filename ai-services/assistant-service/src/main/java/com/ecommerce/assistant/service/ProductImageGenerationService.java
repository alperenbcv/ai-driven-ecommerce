package com.ecommerce.assistant.service;

import java.util.Map;

public interface ProductImageGenerationService {

    Map<String, String> generateProductImage(Map<String, String> body);
}
