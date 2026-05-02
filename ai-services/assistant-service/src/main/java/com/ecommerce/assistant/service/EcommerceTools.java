package com.ecommerce.assistant.service;

import com.ecommerce.assistant.client.OrderClient;
import com.ecommerce.assistant.client.ProductClient;
import com.ecommerce.assistant.client.RecommendationClient;
import com.ecommerce.assistant.dto.tool.OrderSummary;
import com.ecommerce.assistant.dto.tool.ProductSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * E-ticaret Tool'ları — LLM'in gerçek backend'e bağlanmasını sağlar.
 *
 * ═══════════════════════════════════════════════════════════════════
 * @Tool anotasyonu nedir?
 * ═══════════════════════════════════════════════════════════════════
 * Spring AI 1.0'ın Function Calling entegrasyonu.
 *
 * LLM (GPT-4o-mini) bir istekte "tool çağrısı" kararı verebilir.
 * Örneğin kullanıcı "Sony kulaklık var mı?" derse:
 *
 *   1. LLM isteği alır
 *   2. "searchProducts tool'unu çağırayım" kararı verir
 *   3. Spring AI `searchProducts("Sony kulaklık")` methodunu çağırır
 *   4. Sonuç LLM'e geri gönderilir
 *   5. LLM sonucu kullanarak doğal dil cevabı üretir
 *
 * Bu, RAG (Retrieval Augmented Generation) benzeri bir yaklaşım:
 * LLM kendi bilgisiyle değil, gerçek veritabanı verisiyle cevap verir.
 *
 * ═══════════════════════════════════════════════════════════════════
 * description neden önemli?
 * ═══════════════════════════════════════════════════════════════════
 * LLM hangi tool'u ne zaman çağıracağına description'a bakarak karar verir.
 * "Ürün aramak için kullan" vs "Sipariş durumu için kullan" gibi açık
 * tanımlar, LLM'in doğru tool'u seçmesini sağlar.
 * Kötü description → yanlış tool → hatalı cevap.
 *
 * ═══════════════════════════════════════════════════════════════════
 * MCP Server entegrasyonu
 * ═══════════════════════════════════════════════════════════════════
 * spring-ai-starter-mcp-server-webmvc bağımlılığı ile bu tool'lar
 * otomatik olarak /mcp/sse endpoint'i üzerinden dışarıya açılır.
 * Cursor veya başka bir MCP client bu endpoint'e bağlanarak
 * aynı tool'ları kullanabilir.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EcommerceTools {

    private final ProductClient productClient;
    private final OrderClient orderClient;
    private final RecommendationClient recommendationClient;
    private final ProductResultHolder productResultHolder;
    private final UserContextHolder userContextHolder;

    private static final int PRODUCT_BATCH_CHUNK = 80;

    /**
     * Ürün arama — kullanıcının belirttiği kelime ile.
     *
     * LLM ne zaman çağırır?
     * - "X ürünü varmı?"
     * - "Samsung telefon önerir misin?"
     * - "En ucuz laptop ne kadar?"
     * - "Bu kategoride ne var?"
     */
    @Tool(description = """
            E-ticaret platformunda ürün ara. Kullanıcı ürün sorarken,
            belirli bir marka veya kategori araştırırken kullan.
            Sonuçlar ürün adı, fiyat, ortalama puanı ve kısa açıklama içerir.
            """)
    public String searchProducts(
            @ToolParam(description = "Aranacak kelime veya ürün adı, Türkçe olabilir") String query
    ) {
        log.info("[Tool] searchProducts çağrıldı: query={}", query);
        if (query == null || query.isBlank()) {
            return "Arama yapmak için lütfen bir ürün adı veya anahtar kelime belirtin.";
        }
        String keyword = query.trim();
        try {
            var response = productClient.search(keyword, null, 0, 6);
            List<ProductSummary> products = response != null && response.getData() != null
                    ? response.getData().getContent()
                    : List.of();

            if (products.isEmpty()) {
                return "Arama sonucu bulunamadı: " + keyword;
            }

            // Bulunan ürünleri holder'a kaydet — AssistantServiceImpl okuyacak
            productResultHolder.add(products);

            // LLM'e sadece okunabilir özet dön
            return products.stream()
                    .map(p -> "%s — %.2f TRY (Puan: %.1f/5)".formatted(
                            p.getName(),
                            p.getPrice(),
                            p.getAverageRating() != null ? p.getAverageRating().doubleValue() : 0.0))
                    .collect(Collectors.joining("\n"));

        } catch (Exception e) {
            log.error("[Tool] searchProducts hata: {}", e.getMessage());
            return "Ürün araması şu an kullanılamıyor. Lütfen daha sonra tekrar deneyin.";
        }
    }

    /**
     * Belirli bir ürünün detaylarını getir.
     *
     * LLM ne zaman çağırır?
     * - "Bu ürün hakkında daha fazla bilgi ver" (productId varsa)
     * - searchProducts sonrası detay istendiğinde
     */
    @Tool(description = """
            Belirli bir ürünün detaylı bilgilerini getir.
            Önce searchProducts ile ürün ID'sini bul, sonra bu tool'u kullan.
            """)
    public String getProductDetails(
            @ToolParam(description = "Ürünün sayısal ID değeri") Long productId
    ) {
        log.info("[Tool] getProductDetails çağrıldı: productId={}", productId);
        try {
            var resp = productClient.getById(productId);
            ProductSummary p = resp != null ? resp.getData() : null;
            if (p == null) return "Ürün bulunamadı (id=" + productId + ")";
            return """
                    Ürün Adı: %s
                    Marka: %s
                    Kategori: %s
                    Fiyat: %.2f TRY
                    Puan: %.1f/5 (%d yorum)
                    Açıklama: %s
                    """.formatted(
                    p.getName(),
                    p.getBrandName() != null ? p.getBrandName() : "-",
                    p.getCategoryName() != null ? p.getCategoryName() : "-",
                    p.getPrice(),
                    p.getAverageRating() != null ? p.getAverageRating().doubleValue() : 0.0,
                    p.getReviewCount() != null ? p.getReviewCount() : 0,
                    p.getDescription() != null ? p.getDescription() : "-"
            );
        } catch (Exception e) {
            log.error("[Tool] getProductDetails hata: {}", e.getMessage());
            return "Ürün detayı şu an alınamıyor (productId=" + productId + ")";
        }
    }

    @Tool(description = """
            Giriş yapmış kullanıcıya kişiselleştirilmiş ürün önerileri getir.
            Kullanıcı "bana özel öner", "benim için seç", "geçmişime göre öner" gibi
            kişisel tavsiye istediğinde bu tool'u kullan.
            """)
    public String getPersonalizedRecommendations(
            @ToolParam(description = "Döndürülecek öneri sayısı, varsayılan 6") Integer limit
    ) {
        Long userId = userContextHolder.getUserId();
        int safeLimit = limit != null && limit > 0 ? Math.min(limit, 10) : 6;
        log.info("[Tool] getPersonalizedRecommendations çağrıldı: userId={}, limit={}", userId, safeLimit);

        if (userId == null) {
            return "Kişiselleştirilmiş öneriler için giriş yapmanız gerekiyor.";
        }

        try {
            var response = recommendationClient.getForUser(userId, safeLimit);
            List<Long> productIds = response != null && response.getData() != null
                    ? response.getData()
                    : List.of();
            boolean personalized = !productIds.isEmpty();
            List<OrderSummary.OrderItemSummary> recentOrderItems = List.of();

            if (productIds.isEmpty()) {
                recentOrderItems = getRecentOrderItems(userId);
                productIds = recentOrderItems.stream()
                        .map(OrderSummary.OrderItemSummary::getProductId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .flatMap(productId -> getProductBasedRecommendationIds(productId, safeLimit).stream())
                        .distinct()
                        .limit(safeLimit)
                        .toList();
            }

            if (productIds.isEmpty()) {
                var popularResponse = recommendationClient.getPopular(safeLimit);
                productIds = popularResponse != null && popularResponse.getData() != null
                        ? popularResponse.getData()
                        : List.of();
            }

            List<ProductSummary> products = fetchProductsOrdered(productIds, safeLimit);

            if (products.isEmpty()) {
                return "Henüz kişiselleştirilmiş öneri oluşmadı. Birkaç ürün inceledikten veya satın aldıktan sonra daha iyi öneriler sunabilirim.";
            }

            productResultHolder.add(products);

            String prefix;
            if (personalized) {
                prefix = "";
            } else if (!recentOrderItems.isEmpty()) {
                String purchasedProducts = summarizePurchasedProducts(recentOrderItems);
                prefix = "Sipariş geçmişinde " + purchasedProducts
                        + " gördüm. Benzer kullanıcı verisi henüz sınırlı olduğu için buna yakın ve popüler ürünlerden başladım:\n";
            } else {
                prefix = "Henüz sipariş geçmişine dayalı kişisel veri oluşmadığı için popüler ürünlerden başladım:\n";
            }

            return prefix + products.stream()
                    .map(p -> "%s — %.2f TRY (%s)".formatted(
                            p.getName(),
                            p.getPrice(),
                            p.getCategoryName() != null ? p.getCategoryName() : "kategori yok"))
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.error("[Tool] getPersonalizedRecommendations hata: {}", e.getMessage());
            return "Kişiselleştirilmiş öneriler şu an alınamıyor. İstersen ürün adı veya kategoriyle arama yapabilirim.";
        }
    }

    /**
     * Sipariş durumu sorgulama.
     *
     * LLM ne zaman çağırır?
     * - "ORD-20260501-00001 siparişim nerede?"
     * - "Siparişimin durumu ne?"
     * - "Ne zaman teslim edilecek?"
     *
     * userId parametresi: Authorization için zorunlu.
     * LLM bunu context'ten (userId claim) alır, kullanıcı giremez.
     * Bu sayede kullanıcı başkasının siparişini sorgulayamaz.
     */
    @Tool(description = """
            Sipariş numarasına göre sipariş durumunu sorgula.
            Kullanıcı "siparişim nerede" veya belirli bir sipariş numarası sorarsa kullan.
            Sipariş numarası ORD- ile başlar.
            """)
    public String getOrderStatus(
            @ToolParam(description = "Sipariş numarası, ORD- ile başlar (örn: ORD-20260501-00001)") String orderNumber
    ) {
        Long effectiveUserId = userContextHolder.getUserId();
        log.info("[Tool] getOrderStatus çağrıldı: orderNumber={}, userId={}", orderNumber, effectiveUserId);
        if (effectiveUserId == null) {
            return "Sipariş sorgulamak için giriş yapmanız gerekiyor.";
        }
        try {
            var response = orderClient.getByOrderNumber(orderNumber, effectiveUserId);
            OrderSummary order = response != null ? response.getData() : null;
            if (order == null) {
                return "Sipariş bilgisi bulunamadı. Sipariş numarasını kontrol edin: " + orderNumber;
            }

            String statusDesc = switch (order.getStatus()) {
                case "PENDING"        -> "Ödeme bekleniyor";
                case "STOCK_RESERVED" -> "Stok ayrıldı, ödeme işleniyor";
                case "CONFIRMED"      -> "Onaylandı, kargoya hazırlanıyor";
                case "SHIPPED"        -> "Kargoya verildi";
                case "DELIVERED"      -> "Teslim edildi";
                case "CANCELLED"      -> "İptal edildi";
                default               -> order.getStatus();
            };

            StringBuilder sb = new StringBuilder();
            sb.append("Sipariş Durumu: ").append(statusDesc).append("\n");
            sb.append("Sipariş No: ").append(order.getOrderNumber()).append("\n");
            sb.append("Toplam: ").append(order.getTotalAmount()).append(" TRY\n");

            if (order.getCargoTrackingNumber() != null) {
                sb.append("Kargo Takip No: ").append(order.getCargoTrackingNumber()).append("\n");
                sb.append("Kargo Firması: ").append(order.getCargoProvider()).append("\n");
            }
            if (order.getCancelReason() != null) {
                sb.append("İptal Nedeni: ").append(order.getCancelReason()).append("\n");
            }

            return sb.toString();

        } catch (Exception e) {
            log.error("[Tool] getOrderStatus hata: {}", e.getMessage());
            return "Sipariş bilgisi alınamadı. Sipariş numarasını kontrol edin: " + orderNumber;
        }
    }

    /**
     * Platform hakkında genel sorular.
     *
     * Feign çağrısı gerektirmeyen, statik bilgi döndüren tool.
     * LLM bu tür static tool'ları da çağırabilir — tutarlı cevaplar için faydalı.
     */
    @Tool(description = """
            Platform politikaları, iade, kargo süresi, ödeme yöntemleri gibi
            genel sorulara cevap ver. Mağazaya, siparişe veya ürüne özel olmayan
            genel bilgi soruları için kullan.
            """)
    public String getPlatformInfo(
            @ToolParam(description = "Sorulan konu: 'iade', 'kargo', 'odeme', 'guvenlik' gibi") String topic
    ) {
        return switch (topic.toLowerCase()) {
            case "iade"      -> """
                    İade Politikası:
                    - Teslimattan itibaren 14 gün içinde iade hakkınız vardır.
                    - Ürün kullanılmamış ve orijinal ambalajında olmalıdır.
                    - İade talebi hesabınızdan oluşturulabilir.
                    - İade onaylandığında 3-5 iş günü içinde ödeme iade edilir.
                    """;
            case "kargo"     -> """
                    Kargo Bilgisi:
                    - Standart teslimat: 2-4 iş günü
                    - Express teslimat: 1-2 iş günü (ek ücretli)
                    - 500 TL üzeri siparişlerde kargo ücretsizdir.
                    - Kargo takibini Siparişlerim sayfasından yapabilirsiniz.
                    """;
            case "odeme"     -> """
                    Ödeme Yöntemleri:
                    - Kredi / Banka kartı (Visa, Mastercard, Troy)
                    - 12 aya kadar taksit imkânı
                    - Ödeme bilgileriniz Iyzico güvencesindedir.
                    - SSL şifrelemeli güvenli ödeme altyapısı kullanılmaktadır.
                    """;
            case "guvenlik"  -> """
                    Güvenlik:
                    - Tüm veriler SSL/TLS ile şifrelenir.
                    - Şifreler BCrypt ile hash'lenir, düz metin olarak saklanmaz.
                    - Ödeme bilgileri platformda saklanmaz, Iyzico işler.
                    """;
            default          -> """
                    Platformumuz hakkında daha fazla bilgi için yardım merkezimizi ziyaret edebilirsiniz.
                    Ürün arama, sipariş takibi veya iade gibi konularda yardımcı olabilirim.
                    """;
        };
    }

    /**
     * Öneri sırasını koruyarak PostgreSQL ile uyumlu batch üzerinden yükle ({@code /api/products/batch}).
     */
    private List<ProductSummary> fetchProductsOrdered(List<Long> orderedIds, int limit) {
        if (orderedIds == null || orderedIds.isEmpty()) {
            return List.of();
        }
        List<Long> distinctOrdered = orderedIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, ProductSummary> byId = fetchProductsBatchMap(distinctOrdered);
        return distinctOrdered.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .limit(limit > 0 ? limit : distinctOrdered.size())
                .toList();
    }

    private Map<Long, ProductSummary> fetchProductsBatchMap(List<Long> ids) {
        LinkedHashMap<Long, ProductSummary> out = new LinkedHashMap<>();
        if (ids == null || ids.isEmpty()) {
            return out;
        }
        for (int i = 0; i < ids.size(); i += PRODUCT_BATCH_CHUNK) {
            List<Long> chunk = ids.subList(i, Math.min(i + PRODUCT_BATCH_CHUNK, ids.size()));
            try {
                var resp = productClient.batchByIds(chunk);
                List<ProductSummary> list = resp != null && resp.getData() != null
                        ? resp.getData()
                        : List.of();
                for (ProductSummary p : list) {
                    if (p.getId() != null) {
                        out.putIfAbsent(p.getId(), p);
                    }
                }
            } catch (Exception e) {
                log.warn("[Tool] Ürün toplu alınamadı: chunk={}, error={}", chunk, e.toString());
            }
        }
        return out;
    }

    private List<OrderSummary.OrderItemSummary> getRecentOrderItems(Long userId) {
        try {
            var response = orderClient.getMyOrders(userId, 0, 5);
            var page = response != null ? response.getData() : null;
            if (page == null || page.getContent() == null) {
                return List.of();
            }
            return page.getContent().stream()
                    .filter(Objects::nonNull)
                    .flatMap(order -> order.getItems() != null ? order.getItems().stream() : List.<OrderSummary.OrderItemSummary>of().stream())
                    .filter(item -> item.getProductId() != null || item.getProductName() != null)
                    .toList();
        } catch (Exception e) {
            log.warn("[Tool] Son sipariş kalemleri alınamadı: userId={}, error={}", userId, e.getMessage());
            return List.of();
        }
    }

    private List<Long> getProductBasedRecommendationIds(Long productId, int limit) {
        try {
            var response = recommendationClient.getForProduct(productId, limit);
            return response != null && response.getData() != null ? response.getData() : List.of();
        } catch (Exception e) {
            log.warn("[Tool] Ürün bazlı öneri alınamadı: productId={}, error={}", productId, e.getMessage());
            return List.of();
        }
    }

    private String summarizePurchasedProducts(List<OrderSummary.OrderItemSummary> items) {
        Set<String> names = items.stream()
                .map(OrderSummary.OrderItemSummary::getProductName)
                .filter(Objects::nonNull)
                .filter(name -> !name.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (names.isEmpty()) {
            return "son siparişlerini";
        }

        String summary = names.stream().limit(3).collect(Collectors.joining(", "));
        if (names.size() > 3) {
            summary += " ve diğer ürünleri";
        }
        return summary;
    }
}
