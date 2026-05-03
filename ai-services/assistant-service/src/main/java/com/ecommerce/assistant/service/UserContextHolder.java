package com.ecommerce.assistant.service;

import org.springframework.stereotype.Component;


/**
 * Chat isteği boyunca mevcut kullanıcı ID'sini geçici olarak tutan yardımcı sınıftır.
 *
 * AssistantServiceImpl.chat() metodu controller'dan gelen userId bilgisini alır.
 * Ancak Spring AI tool metodları doğrudan controller parametrelerine erişemez.
 * Örneğin EcommerceTools içinde "kullanıcının siparişlerini getir" veya
 * "kişisel öneri üret" gibi işlemler yapılırken mevcut kullanıcının ID'sine ihtiyaç duyulur.
 *
 * Bu sınıf, userId bilgisini tool katmanına taşımak için kullanılır.
 *
 * Akış:
 * 1. Controller, X-User-Id header'ını okur ve AssistantService.chat(request, userId) metoduna gönderir.
 * 2. AssistantServiceImpl.chat() isteğin başında setUserId(userId) çağırır.
 * 3. LLM bir tool çağırdığında, tool içinden getUserId() ile mevcut kullanıcı ID'si alınabilir.
 * 4. Chat işlemi tamamlanınca clear() çağrılır.
 *
 */
@Component
public class UserContextHolder {

    private final ThreadLocal<Long> currentUserId = new ThreadLocal<>();

    public void setUserId(Long userId) {
        currentUserId.set(userId);
    }

    public Long getUserId() {
        return currentUserId.get();
    }

    public void clear() {
        currentUserId.remove();
    }
}