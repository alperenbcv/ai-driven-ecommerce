package com.ecommerce.product.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserServiceClient {

    private final RestTemplate restTemplate;

    private final Map<Long, String> nameCache = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public String getSellerDisplayName(Long sellerId) {
        if (sellerId == null) return "Bilinmeyen Satıcı";
        return nameCache.computeIfAbsent(sellerId, id -> {
            try {
                Map<String, Object> response = restTemplate.getForObject(
                        "http://user-service/api/users/{id}/public",
                        Map.class,
                        id
                );
                if (response != null && response.containsKey("data")) {
                    Map<String, Object> data = (Map<String, Object>) response.get("data");
                    Object name = data.get("displayName");
                    return name != null ? name.toString() : "Satıcı #" + id;
                }
                return "Satıcı #" + id;
            } catch (Exception e) {
                log.warn("Seller adı alınamadı: sellerId={}, hata={}", id, e.getMessage());
                return "Satıcı #" + id;
            }
        });
    }
}
