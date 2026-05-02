package com.ecommerce.assistant.service;

import java.util.Map;

/** DALL-E vb. görsel üretimi — kullanıcıya ham exception mesajı sızdırılmaz. */
public interface ProductImageGenerationService {

    Map<String, String> generateProductImage(Map<String, String> body);
}
