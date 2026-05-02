package com.ecommerce.assistant.service;

import com.ecommerce.assistant.dto.tool.ProductSummary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * AI chat akışı sırasında tool'ların bulduğu ürün sonuçlarını geçici olarak tutan yardımcı sınıftır.
 *
 * Spring AI tool çağrıları chat cevabı üretilirken arka planda çalışır.
 * EcommerceTools.searchProducts gibi bir tool ürünleri bulur ancak bu ürünleri doğrudan
 * ChatResponse içine koyamaz. Çünkü tool sadece LLM'e bilgi sağlar; HTTP response'u ise
 * AssistantServiceImpl tarafından oluşturulur.
 *
 * Bu nedenle ProductResultHolder ara bir köprü görevi görür:
 *
 * Akış:
 * 1. AssistantServiceImpl.chat() her yeni isteğin başında clear() çağırır.
 * 2. LLM, ürün arama tool'unu çağırırsa EcommerceTools.searchProducts ürünleri bulur.
 * 3. Bulunan ProductSummary listesi add() ile bu holder içine yazılır.
 * 4. Chat cevabı üretildikten sonra AssistantServiceImpl get() ile ürünleri alır.
 * 5. Bu ürünler ChatResponse.products alanına eklenir ve frontend'de kart olarak gösterilir.
 * 6. İşlem sonunda clear() çağrılarak aynı thread'de eski ürünlerin kalması engellenir.
 *
 * ThreadLocal neden kullanıldı:
 * Her HTTP isteği farklı bir thread üzerinde çalışabilir. ThreadLocal sayesinde ürün listesi
 * global shared state olarak değil, sadece o isteği işleyen thread'e özel tutulur.
 * Böylece aynı anda gelen farklı kullanıcı isteklerinin ürün sonuçları birbirine karışmaz.
 *
 */
@Component
public class ProductResultHolder {

    private final ThreadLocal<List<ProductSummary>> products =
            ThreadLocal.withInitial(ArrayList::new);

    public void add(List<ProductSummary> found) {
        products.get().addAll(found);
    }

    public List<ProductSummary> get() {
        return List.copyOf(products.get());
    }

    public void clear() {
        products.remove();
    }
}
