package com.ecommerce.order.client;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.NotFoundException;
import lombok.Builder;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Product Service çağrıları için manuel HTTP client yaklaşımı kullandım.
 *
 * Bu sınıf Order Service'in sipariş oluşturma aşamasında Product Service'ten
 * ürün ve listing bilgilerini doğrulamak için kullandığı küçük bir client sınıfıdır.
 *
 * Burada Feign yerine RestTemplate kullanılmasının sebebi tamamen tercihendir,
 * Feign kullanılması tutarlılık açısından daha doğru olurdu.
 *
 * Temel amaç:
 * - Siparişe eklenen productId gerçekten var mı kontrol etmek.
 * - Ürünün aktif olup olmadığını öğrenmek.
 * - Kullanıcının seçtiği listingId gerçekten o ürüne ait mi kontrol etmek.
 * - Listing fiyatı, sellerId ve aktiflik bilgisini Order Service tarafına taşımak.
 *
 * getProduct:
 * Product Service'teki /api/products/{id} endpoint'ine istek atar.
 * Dönen ApiResponse içindeki data alanından ürünün id, name, price ve active
 * bilgilerini çıkarır ve ProductSnapshot nesnesine dönüştürür.
 *
 * getListing:
 * Product Service'teki /api/products/{id}/listings endpoint'ine istek atar.
 * Ürüne ait tüm seller listing'lerini getirir, verilen listingId ile eşleşeni bulur
 * ve ProductListingSnapshot olarak döndürür. Eğer listing bulunamazsa NotFoundException
 * fırlatılır.
 *
 * dataMap, asString, asLong, asBigDecimal:
 * RestTemplate ile Map olarak alınan generic response'u güvenli şekilde okumak için
 * kullanılan yardımcı metotlardır. Çünkü burada typed Feign DTO yerine manuel Map parsing
 * yapıldığı için gelen Object değerlerinin Long, String ve BigDecimal tiplerine çevrilmesi gerekir.
 *
 * ProductSnapshot:
 * Order Service'in ürün doğrulaması için ihtiyaç duyduğu minimum ürün bilgisini taşır.
 * Tüm ProductResponse'u almak yerine sadece sipariş için gerekli alanlar tutulur.
 *
 * ProductListingSnapshot:
 * Siparişte kullanılacak satıcı listing bilgisinin küçük bir özetidir.
 * Order item oluştururken listingId, sellerId, productName, price ve active bilgileri
 * bu nesne üzerinden kullanılır.
 */

@Component
public class ProductCatalogClient {

    private final RestTemplate restTemplate;
    private final String productServiceUrl;

    public ProductCatalogClient(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${services.product-service.url:http://localhost:8082}") String productServiceUrl) {
        this.restTemplate = restTemplateBuilder.build();
        this.productServiceUrl = productServiceUrl.replaceAll("/+$", "");
    }

    public ProductSnapshot getProduct(Long productId) {
        try {
            Map<?, ?> response = restTemplate.getForObject(
                    productServiceUrl + "/api/products/{id}",
                    Map.class,
                    productId
            );
            Map<?, ?> data = dataMap(response);
            return ProductSnapshot.builder()
                    .id(asLong(data.get("id")))
                    .name(asString(data.get("name")))
                    .price(asBigDecimal(data.get("price")))
                    .active(Boolean.TRUE.equals(data.get("active")))
                    .build();
        } catch (NotFoundException | BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            throw new BusinessException("Ürün servisine ulaşılamadı: " + e.getMessage());
        }
    }

    public ProductListingSnapshot getListing(Long productId, Long listingId) {
        try {
            Map<?, ?> response = restTemplate.getForObject(
                    productServiceUrl + "/api/products/{id}/listings",
                    Map.class,
                    productId
            );
            Object data = response != null ? response.get("data") : null;
            if (!(data instanceof List<?> listings)) {
                throw new NotFoundException("Ürün listing bilgisi alınamadı: productId=" + productId);
            }

            return listings.stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .filter(listing -> listingId.equals(asLong(listing.get("id"))))
                    .findFirst()
                    .map(listing -> ProductListingSnapshot.builder()
                            .id(asLong(listing.get("id")))
                            .productId(asLong(listing.get("productId")))
                            .productName(asString(listing.get("productName")))
                            .sellerId(asLong(listing.get("sellerId")))
                            .price(asBigDecimal(listing.get("price")))
                            .active(Boolean.TRUE.equals(listing.get("active")))
                            .build())
                    .orElseThrow(() -> new NotFoundException(
                            "Aktif ürün listing'i bulunamadı: productId=%d, listingId=%d"
                                    .formatted(productId, listingId)));
        } catch (NotFoundException | BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            throw new BusinessException("Ürün listing servisine ulaşılamadı: " + e.getMessage());
        }
    }

    private Map<?, ?> dataMap(Map<?, ?> response) {
        if (response == null || !(response.get("data") instanceof Map<?, ?> data)) {
            throw new NotFoundException("Ürün bilgisi alınamadı");
        }
        return data;
    }

    private static String asString(Object value) {
        return value != null ? value.toString() : null;
    }

    private static Long asLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        return Long.valueOf(value.toString());
    }

    private static BigDecimal asBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) return new BigDecimal(number.toString());
        return new BigDecimal(value.toString());
    }

    @Getter
    @Builder
    public static class ProductSnapshot {
        private Long id;
        private String name;
        private BigDecimal price;
        private boolean active;
    }

    @Getter
    @Builder
    public static class ProductListingSnapshot {
        private Long id;
        private Long productId;
        private String productName;
        private Long sellerId;
        private BigDecimal price;
        private boolean active;
    }
}
