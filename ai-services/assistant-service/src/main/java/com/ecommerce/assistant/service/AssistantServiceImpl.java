package com.ecommerce.assistant.service;

import com.ecommerce.assistant.dto.ChatRequest;
import com.ecommerce.assistant.dto.ChatResponse;
import com.ecommerce.assistant.dto.ProductDescriptionRequest;
import com.ecommerce.assistant.dto.ProductDescriptionResponse;
import com.ecommerce.assistant.dto.tool.ProductSummary;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI Asistan Servisi — Spring AI ChatClient + Tool Calling.
 *
 * Konuşma belleği Redis'e basit JSON string olarak kaydedilir.
 * Spring AI'ın mesaj nesneleri (UserMessage, AssistantMessage)
 * Jackson ile serialize edilemiyor (no-arg constructor yok),
 * bu yüzden [{"role":"user","content":"..."}, ...] formatı kullanıyoruz.
 */
@Service
@Slf4j
public class AssistantServiceImpl implements AssistantService {

    private final ChatClient chatClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final EcommerceTools ecommerceTools;
    private final ProductResultHolder productResultHolder;
    private final UserContextHolder userContextHolder;
    private final ObjectMapper objectMapper;

    @Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}")
    private String chatModelName;

    public AssistantServiceImpl(
            ChatClient chatClient,
            StringRedisTemplate stringRedisTemplate,
            EcommerceTools ecommerceTools,
            ProductResultHolder productResultHolder,
            UserContextHolder userContextHolder,
            ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.stringRedisTemplate = stringRedisTemplate;
        this.ecommerceTools = ecommerceTools;
        this.productResultHolder = productResultHolder;
        this.userContextHolder = userContextHolder;
        this.objectMapper = objectMapper;
    }

    private static final String SESSION_KEY_PREFIX = "chat:";
    private static final int MAX_HISTORY_SIZE = 20;
    private static final Duration SESSION_TTL = Duration.ofMinutes(30);

    private static final String SYSTEM_PROMPT = """
            Sen EShop platformunun yapay zeka destekli alışveriş asistanısın.
            Gerçek veri tabanına erişen tool'larla çalışırsın — asla tahmin etme, her zaman tool çağır.

            ARAÇLARIN (tool'ların):
            • searchProducts       → Ürün adı/marka/kategori araması
            • getProductDetails    → Belirli bir ürünün detaylarını getir
            • getPersonalizedRecommendations → Kullanıcının satın alma ve görüntüleme geçmişine dayalı kişisel öneri
            • getOrderStatus       → Sipariş numarasıyla durum sorgulama (ORD- ile başlar)
            • getPlatformInfo      → İade, kargo, ödeme politikaları

            KARAR KURALLARI:
            1. Ürün veya marka sorusu → searchProducts ÇAĞIR
            2. "Bana öner", "ne almalıyım", "beğenebileceğim", "kişisel" → getPersonalizedRecommendations ÇAĞIR
            3. Sipariş numarası geldi → getOrderStatus ÇAĞIR
            4. İade / kargo / ödeme / politika → getPlatformInfo ÇAĞIR
            5. Kullanıcı öneriyi beğenmedi / daralt/genişlet istedi → aynı veya farklı tool'u tekrar çağır

            YANIT FORMATI:
            - Her zaman Türkçe
            - Kısa ve net; madde listesiyle değil, akıcı cümlelerle
            - Ürün önerisi geldiğinde isimlerini öne çıkar, karşılaştırma yap
            - E-ticaret dışı sorularda kibarca konu dışı olduğunu belirt
            """;

    @Override
    public ChatResponse chat(ChatRequest request, Long userId) {
        String sessionId = request.getSessionId() != null
                ? request.getSessionId()
                : UUID.randomUUID().toString();

        // Redis'ten önceki konuşmayı yükle (kullanıcıya göre ayrılmış anahtar)
        List<Message> history = loadHistory(sessionId, userId);

        // Kullanıcı mesajını ekle
        history.add(new UserMessage(request.getMessage()));

        // GPT'ye gönderilecek tam mesaj listesi (system + geçmiş)
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(SYSTEM_PROMPT));
        messages.addAll(history);

        try {
            // Her istek öncesi holder'ı temizle, kullanıcı kimliğini set et
            productResultHolder.clear();
            userContextHolder.setUserId(userId);

            // LLM hangi tool'u çağıracağına kendisi karar verir — keyword bypass yok
            String reply = chatClient.prompt()
                    .messages(messages)
                    .tools(ecommerceTools)
                    .call()
                    .content();

            List<ProductSummary> foundProducts = productResultHolder.get();
            productResultHolder.clear();

            history.add(new AssistantMessage(reply));

            if (history.size() > MAX_HISTORY_SIZE) {
                history = history.subList(history.size() - MAX_HISTORY_SIZE, history.size());
            }

            saveHistory(sessionId, userId, history);

            return ChatResponse.builder()
                    .sessionId(sessionId)
                    .reply(reply)
                    .products(foundProducts.isEmpty() ? null : foundProducts)
                    .model(chatModelName != null ? chatModelName : "unknown")
                    .build();

        } catch (Exception e) {
            log.error("AI yanıt üretilemedi: {}", e.getMessage());
            return ChatResponse.builder()
                    .sessionId(sessionId)
                    .reply("Şu an yardımcı olamıyorum, lütfen daha sonra tekrar deneyin.")
                    .model(chatModelName != null ? chatModelName : "unknown")
                    .build();
        } finally {
            productResultHolder.clear();
            userContextHolder.clear();
        }
    }

    @Override
    public ProductDescriptionResponse generateProductDescription(ProductDescriptionRequest request) {
        String userPrompt = """
                Ürün adı: %s
                Kategori: %s
                Marka: %s
                Mevcut açıklama: %s
                """.formatted(
                request.getProductName(),
                valueOrDash(request.getCategoryName()),
                valueOrDash(request.getBrandName()),
                valueOrDash(request.getCurrentDescription())
        );

        try {
            String content = chatClient.prompt()
                    .system("""
                            Sen bir e-ticaret ürün içerik uzmanısın.
                            Türkçe, satış odaklı ama abartısız bir ürün açıklaması üret.
                            Sadece geçerli JSON döndür. Markdown kullanma.
                            Şema:
                            {
                              "seoTitle": "70 karakteri aşmayan başlık",
                              "description": "2-3 kısa paragraf ürün açıklaması",
                              "tags": ["etiket1", "etiket2", "etiket3", "etiket4", "etiket5"]
                            }
                            """)
                    .user(userPrompt)
                    .call()
                    .content();

            JsonNode root = objectMapper.readTree(stripJsonFence(content));
            List<String> tags = root.path("tags").isArray()
                    ? objectMapper.convertValue(root.path("tags"), new TypeReference<>() {})
                    : List.of();

            return ProductDescriptionResponse.builder()
                    .seoTitle(root.path("seoTitle").asText(request.getProductName()))
                    .description(root.path("description").asText())
                    .tags(tags)
                    .build();
        } catch (Exception e) {
            log.warn("AI ürün açıklaması üretilemedi: {}", e.getMessage());
            return ProductDescriptionResponse.builder()
                    .seoTitle(request.getProductName())
                    .description(valueOrDash(request.getCurrentDescription()).equals("-")
                            ? request.getProductName() + " için kaliteli, güvenilir ve günlük kullanıma uygun bir ürün seçeneği."
                            : request.getCurrentDescription())
                    .tags(List.of())
                    .build();
        }
    }

    @Override
    public void clearSession(String sessionId, Long userId) {
        stringRedisTemplate.delete(chatRedisKey(sessionId, userId));
    }

    /**
     * Oturum anahtarı kullanıcıya göre scope'lanır; giriş yoksa {@code anon} dilimi.
     */
    private static String chatRedisKey(String sessionId, Long userId) {
        String owner = userId != null ? Long.toString(userId) : "anon";
        return SESSION_KEY_PREFIX + owner + ":" + sessionId;
    }

    // ─── Redis: String JSON ↔ Spring AI Message ───────────────────────────────

    /**
     * Redis'ten [{"role":"user","content":"..."},...] formatında okur,
     * Spring AI mesaj nesnelerine dönüştürür.
     */
    private List<Message> loadHistory(String sessionId, Long userId) {
        try {
            String json = stringRedisTemplate.opsForValue().get(chatRedisKey(sessionId, userId));
            if (json == null || json.isBlank()) return new ArrayList<>();

            List<Map<String, String>> records = objectMapper.readValue(
                    json, new TypeReference<>() {});

            List<Message> messages = new ArrayList<>();
            for (Map<String, String> rec : records) {
                String role    = rec.getOrDefault("role", "user");
                String content = rec.getOrDefault("content", "");
                if ("assistant".equals(role)) {
                    messages.add(new AssistantMessage(content));
                } else {
                    messages.add(new UserMessage(content));
                }
            }
            return messages;
        } catch (Exception e) {
            log.warn("Konuşma geçmişi okunamadı: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Spring AI mesaj listesini [{"role":"...","content":"..."},...] olarak
     * JSON string'e çevirip Redis'e yazar.
     */
    private void saveHistory(String sessionId, Long userId, List<Message> history) {
        try {
            List<Map<String, String>> records = history.stream()
                    .filter(m -> m instanceof UserMessage || m instanceof AssistantMessage)
                    .map(m -> Map.of(
                            "role", m instanceof AssistantMessage ? "assistant" : "user",
                            "content", m.getText()
                    ))
                    .toList();

            String json = objectMapper.writeValueAsString(records);
            stringRedisTemplate.opsForValue().set(chatRedisKey(sessionId, userId), json, SESSION_TTL);
        } catch (Exception e) {
            log.warn("Konuşma geçmişi kaydedilemedi: {}", e.getMessage());
        }
    }

    private String valueOrDash(String value) {
        return value != null && !value.isBlank() ? value : "-";
    }

    private String stripJsonFence(String content) {
        if (content == null) {
            return "{}";
        }
        return content
                .replaceFirst("^```json\\s*", "")
                .replaceFirst("^```\\s*", "")
                .replaceFirst("\\s*```$", "")
                .trim();
    }
}
