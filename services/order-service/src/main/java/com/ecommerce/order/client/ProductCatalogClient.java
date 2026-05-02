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
