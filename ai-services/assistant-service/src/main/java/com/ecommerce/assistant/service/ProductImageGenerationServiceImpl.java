package com.ecommerce.assistant.service;

import com.ecommerce.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Ürün görseli üretmek için kullanılan AI servis implementasyonudur.
 *
 * ImageModel:
 * Spring AI'ın görsel üretim modelleri için sağladığı ortak abstraction'dır.
 * Böylece doğrudan OpenAI API çağrısı yazmak yerine Spring AI üzerinden model çağrısı yapılır.
 *
 * generateProductImage akışı:
 * 1. Controller'dan gelen body içinden productName, description ve categoryName okunur.
 * 2. productName boşsa görsel üretilemeyeceği için BusinessException fırlatılır.
 * 3. Ürün adına, kategoriye ve kısa açıklamaya göre DALL-E için prompt hazırlanır.
 * 4. ImagePrompt ile modele gönderilecek prompt ve model ayarları birleştirilir.
 * 5. OpenAiImageOptions ile kullanılacak model, görsel boyutu, adet ve response format belirlenir.
 * 6. Model URL formatında bir görsel sonucu döndürür.
 * 7. Bu URL frontend'e imageUrl olarak geri verilir.
 *
 * OpenAiImageOptions içinde:
 * - model("dall-e-2") → kullanılacak image generation modeli.
 * - N(1)              → tek görsel üretileceğini belirtir.
 * - width/height      → 512x512 kare ürün görseli üretir.
 * - responseFormat("url") → görsel binary olarak değil, erişilebilir URL olarak döner.
 *
 * 
 * dall-e-2 kalite olarak çok kötü bir tercih olmasına rağmen, fiyat açısından çok uygun olduğu için kullanıldı.
 * 
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductImageGenerationServiceImpl implements ProductImageGenerationService {

    private final ImageModel imageModel;

    @Override
    public Map<String, String> generateProductImage(Map<String, String> body) {
        String productName  = body.getOrDefault("productName", "").strip();
        String description  = body.getOrDefault("description", "");
        String categoryName = body.getOrDefault("categoryName", "");

        if (productName.isEmpty()) {
            throw new BusinessException("Ürün adı gereklidir");
        }

        String prompt = String.format(
                "E-commerce product photo, clean white background, professional studio lighting. "
                        + "Product: %s. Category: %s. %s. "
                        + "No text, no watermarks, photorealistic, high quality.",
                productName,
                categoryName.isBlank() ? "general" : categoryName,
                description.isBlank() ? ""
                        : "Details: " + description.substring(0, Math.min(description.length(), 200))
        );

        log.info("DALL-E 2 görsel üretiliyor: {}", productName);

        try {
            ImageResponse response = imageModel.call(new ImagePrompt(prompt,
                    OpenAiImageOptions.builder()
                            .model("dall-e-2")
                            .N(1)
                            .width(512)
                            .height(512)
                            .responseFormat("url")
                            .build()));

            String imageUrl = response.getResult().getOutput().getUrl();
            log.info("DALL-E 2 görsel üretildi: {}", productName);
            return Map.of("imageUrl", imageUrl);
        } catch (Exception e) {
            log.error("DALL-E 2 görsel üretme hatası", e);
            throw new BusinessException("Ürün görseli oluşturulamadı.", "GENERATION_FAILED");
        }
    }
}
