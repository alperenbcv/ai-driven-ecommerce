package com.ecommerce.assistant.service;

import com.ecommerce.assistant.dto.ChatRequest;
import com.ecommerce.assistant.dto.ChatResponse;
import com.ecommerce.assistant.dto.ProductDescriptionRequest;
import com.ecommerce.assistant.dto.ProductDescriptionResponse;

/**
 *
 *
 * Neden doğrudan AssistantService class'ı kullanmadım?
 *
 * Controller katmanının somut sınıfa değil, interface'e bağımlı olması daha temiz bir tasarımdır.
 * Yani AssistantController şunu bilmek zorunda kalmaz:
 *
 * Controller sadece AssistantService'in hangi methodları sağladığını bilir.
 * Loose coupling şeklinde yazmak daha kullanışlı geldiği için bu şekilde tercih ettim.
 */
public interface AssistantService {

    ChatResponse chat(ChatRequest request, Long userId);

    void clearSession(String sessionId, Long userId);

    ProductDescriptionResponse generateProductDescription(ProductDescriptionRequest request);
}
