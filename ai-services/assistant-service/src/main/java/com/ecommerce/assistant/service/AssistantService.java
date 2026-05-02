package com.ecommerce.assistant.service;

import com.ecommerce.assistant.dto.ChatRequest;
import com.ecommerce.assistant.dto.ChatResponse;
import com.ecommerce.assistant.dto.ProductDescriptionRequest;
import com.ecommerce.assistant.dto.ProductDescriptionResponse;

/** AI chat ve ürün metni işlemleri. */
public interface AssistantService {

    ChatResponse chat(ChatRequest request, Long userId);

    void clearSession(String sessionId, Long userId);

    ProductDescriptionResponse generateProductDescription(ProductDescriptionRequest request);
}
