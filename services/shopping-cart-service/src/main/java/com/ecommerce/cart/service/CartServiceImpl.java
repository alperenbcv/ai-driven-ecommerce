package com.ecommerce.cart.service;

import com.ecommerce.cart.dto.AddToCartRequest;
import com.ecommerce.cart.dto.CartItem;
import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 *
 * Redis key şeması:
 *   "cart:{userId}"  →  Hash
 *   Hash field key: "{productId}:{listingId}"
 *   Hash field value: CartItem (JSON)
 *
 * Örnek:
 *   cart:42  →  {
 *     "101:5001": { productId:101, listingId:5001, qty:2, price:299.90 }
 *     "102:5002": { productId:102, listingId:5002, qty:1, price:149.00 }
 *   }
 *
 * TTL
 *   Sepet 7 gün kullanılmazsa otomatik silinir.
 *   Her ekleme-güncelleme TTL'yi sıfırlar.
 *   Bu sayede aktif kullanıcıların sepeti kalır, hayalet sepetler temizlenir.
 *
 * Operasyonlar:
 *   HSET  → ürün ekle/güncelle (O(1))
 *   HGET  → tek ürün getir (O(1))
 *   HGETALL → tüm sepeti getir (O(n))
 *   HDEL  → ürün çıkar (O(1))
 *   DEL   → sepeti temizle (O(1))
 *   EXPIRE → TTL güncelle
 *
 * Neden DB değil :
 *   - Sepet geçici veri, kalıcı olması şart değil
 *   - Çok sık yazma-okuma operasyonu DB baskı altında kalır
 *   - Redis memory'de çalışır mikrosaniye yanıt süreleri
 *   - TTL desteği built-in
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl {

    private final RedisTemplate<String, CartItem> cartRedisTemplate;

    private static final String CART_KEY_PREFIX = "cart:";
    private static final long CART_TTL_DAYS = 7;

    private String cartKey(Long userId) {
        return CART_KEY_PREFIX + userId;
    }

    private String itemKey(Long productId, Long listingId) {
        return productId + ":" + listingId;
    }

    /**
     * Sepete ürün ekle veya mevcut adedi artır.
     * ürün zaten ekliyse quantity artırılır.
     */
    public CartResponse addItem(Long userId, AddToCartRequest request) {
        String key = cartKey(userId);
        String field = itemKey(request.getProductId(), request.getListingId());

        CartItem existing = (CartItem) cartRedisTemplate.opsForHash().get(key, field);

        CartItem item;
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + request.getQuantity());
            existing.setUnitPrice(request.getUnitPrice());
            item = existing;
        } else {
            item = CartItem.builder()
                    .productId(request.getProductId())
                    .listingId(request.getListingId())
                    .sellerId(request.getSellerId())
                    .productName(request.getProductName())
                    .unitPrice(request.getUnitPrice())
                    .quantity(request.getQuantity())
                    .build();
        }

        cartRedisTemplate.opsForHash().put(key, field, item);
        cartRedisTemplate.expire(key, CART_TTL_DAYS, TimeUnit.DAYS);

        log.debug("Sepete eklendi: userId={}, product={}, qty={}", userId, request.getProductId(), request.getQuantity());
        return getCart(userId);
    }

    public CartResponse updateItemQuantity(Long userId, Long productId, Long listingId, int quantity) {
        if (quantity < 0) throw new BusinessException("Adet 0'dan küçük olamaz");

        String key = cartKey(userId);
        String field = itemKey(productId, listingId);

        if (quantity == 0) {
            cartRedisTemplate.opsForHash().delete(key, field);
            log.debug("Sepetten silindi: userId={}, product={}", userId, productId);
        } else {
            CartItem item = (CartItem) cartRedisTemplate.opsForHash().get(key, field);
            if (item == null) throw new BusinessException("Ürün sepette bulunamadı");
            item.setQuantity(quantity);
            cartRedisTemplate.opsForHash().put(key, field, item);
        }

        cartRedisTemplate.expire(key, CART_TTL_DAYS, TimeUnit.DAYS);
        return getCart(userId);
    }

    public CartResponse removeItem(Long userId, Long productId, Long listingId) {
        String key = cartKey(userId);
        cartRedisTemplate.opsForHash().delete(key, itemKey(productId, listingId));
        cartRedisTemplate.expire(key, CART_TTL_DAYS, TimeUnit.DAYS);
        return getCart(userId);
    }

    public CartResponse getCart(Long userId) {
        String key = cartKey(userId);
        Map<Object, Object> entries = cartRedisTemplate.opsForHash().entries(key);

        List<CartItem> items = entries.values().stream()
                .filter(v -> v instanceof CartItem)
                .map(v -> (CartItem) v)
                .toList();

        BigDecimal totalAmount = items.stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalQuantity = items.stream().mapToInt(CartItem::getQuantity).sum();

        return CartResponse.builder()
                .userId(userId)
                .items(items)
                .totalItems(items.size())
                .totalQuantity(totalQuantity)
                .totalAmount(totalAmount)
                .build();
    }

    public void clearCart(Long userId) {
        cartRedisTemplate.delete(cartKey(userId));
        log.info("Sepet temizlendi: userId={}", userId);
    }

    public boolean isEmpty(Long userId) {
        Long size = cartRedisTemplate.opsForHash().size(cartKey(userId));
        return size == null || size == 0;
    }
}
