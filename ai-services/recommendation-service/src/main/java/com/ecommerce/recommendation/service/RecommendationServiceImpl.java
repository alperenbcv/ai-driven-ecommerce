package com.ecommerce.recommendation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Öneri Servisi — Neo4j Driver (raw Cypher) tabanlı.
 *
 * Spring Data Neo4j repository yerine raw Driver kullandık çünkü:
 * repository'nin @Transactional altyapısı (transactionTemplate) Spring context
 * başlatılırken henüz hazır olmayabiliyor ve NullPointerException'a yol açıyor.
 * Driver API'si bu sorundan tamamen bağımsızdır.
 *
 * Graf üzerinde tutulan Product.productId değerleri, Product Service PostgreSQL
 * PRIMARY KEY ile aynı kimlikleri taşımalıdır (ürün oluşturulurken / seed ile).
 * RecommendationService çağrılarında kullanıcıya dönen ID listeleri doğrudan
 * PostgreSQL'e batch sorgusu ile doğrulanabilir ({@code /api/products/batch}).
 *
 * Üç öneri stratejisi:
 *
 * 1. Collaborative Filtering — "Seni gibi kullanıcılar şunları aldı"
 *    (u:User)-[:PURCHASED]->(p)<-[:PURCHASED]-(other)-[:PURCHASED]->(rec)
 *
 * 2. Category-Based — "Aynı kategoriden popüler ürünler"
 *    Collaborative yetersiz kalırsa tamamlayıcı olarak kullanılır.
 *
 * 3. Item-Based — "Bunu alanlar bunu da aldı"
 *    Ürün detay sayfasında gösterilir.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {

    private static final int LIMIT_HARD_CAP = 50;

    private final Driver driver;

    // ─── Satın alma kaydı ─────────────────────────────────────────────────────

    @Override
    public void recordPurchase(Map<String, Object> orderEvent) {
        Long userId = Long.valueOf(orderEvent.get("userId").toString());
        String userName = orderEvent.getOrDefault("userName", "").toString();

        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                tx.run("MERGE (u:User {userId: $id}) SET u.name = $name",
                        Values.parameters("id", userId, "name", userName));

                if (orderEvent.get("items") instanceof List<?> items) {
                    for (Object itemObj : items) {
                        if (itemObj instanceof Map<?, ?> rawItem) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> item = (Map<String, Object>) rawItem;
                            Long productId = Long.valueOf(item.get("productId").toString());
                            String productName = item.getOrDefault("name", "").toString();
                            String category = item.getOrDefault("category", "").toString();

                            tx.run("""
                                MERGE (p:Product {productId: $pid})
                                SET p.name = $name, p.category = $cat
                                """, Values.parameters("pid", productId, "name", productName, "cat", category));

                            tx.run("""
                                MATCH (u:User {userId: $uid}), (p:Product {productId: $pid})
                                MERGE (u)-[:PURCHASED]->(p)
                                """, Values.parameters("uid", userId, "pid", productId));
                        }
                    }
                }
                return null;
            });
        }
        log.info("Satın alma graafa eklendi: userId={}", userId);
    }

    // ─── Görüntüleme kaydı ───────────────────────────────────────────────────

    @Override
    public void recordView(Long userId, Long productId, String productName, String category) {
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                tx.run("MERGE (u:User {userId: $id})", Values.parameters("id", userId));
                tx.run("""
                    MERGE (p:Product {productId: $pid})
                    SET p.name = coalesce($name, p.name, 'Bilinmiyor'),
                        p.category = coalesce($cat, p.category, '')
                    """, Values.parameters("pid", productId,
                        "name", productName != null ? productName : null,
                        "cat", category != null ? category : null));
                tx.run("""
                    MATCH (u:User {userId: $uid}), (p:Product {productId: $pid})
                    MERGE (u)-[r:VIEWED]->(p)
                    ON CREATE SET r.count = 1, r.firstViewedAt = datetime()
                    ON MATCH  SET r.count = r.count + 1, r.lastViewedAt = datetime()
                    """, Values.parameters("uid", userId, "pid", productId));
                return null;
            });
            log.debug("VIEWED kaydedildi: userId={}, productId={}", userId, productId);
        } catch (Exception e) {
            log.warn("VIEWED kaydedilemedi (userId={}, productId={}): {}", userId, productId, e.getMessage());
        }
    }

    // ─── Kullanıcı bazlı öneri ────────────────────────────────────────────────

    @Override
    public List<Long> getRecommendationsForUser(Long userId, int limit) {
        int normLimit = normalizeLimit(limit, 10);
        try (Session session = driver.session()) {
            // 1. Collaborative filtering
            List<Long> collaborative = session.run("""
                MATCH (u:User {userId: $uid})-[:PURCHASED]->(p)<-[:PURCHASED]-(other)
                      -[:PURCHASED]->(rec)
                WHERE NOT (u)-[:PURCHASED]->(rec)
                RETURN rec.productId AS productId, count(other) AS score
                ORDER BY score DESC
                LIMIT $limit
                """, Values.parameters("uid", userId, "limit", normLimit))
                    .list(r -> r.get("productId").asLong());

            if (collaborative.size() >= normLimit) return collaborative;

            // 2. Category-based tamamlayıcı
            int remaining = normLimit - collaborative.size();
            List<Long> categoryBased = session.run("""
                MATCH (u:User {userId: $uid})-[:PURCHASED]->(bought)
                WITH u, collect(bought.category) AS cats
                MATCH (rec:Product)
                WHERE rec.category IN cats AND NOT (u)-[:PURCHASED]->(rec)
                WITH DISTINCT rec.productId AS productId
                LIMIT $remaining
                RETURN productId
                """, Values.parameters("uid", userId, "remaining", remaining))
                    .list(r -> r.get("productId").asLong())
                    .stream()
                    .filter(id -> !collaborative.contains(id))
                    .toList();

            return Stream.concat(collaborative.stream(), categoryBased.stream())
                    .distinct()
                    .limit(normLimit)
                    .toList();

        } catch (Exception e) {
            log.warn("Kullanıcı önerisi alınamadı (userId={}): {}", userId, e.getMessage());
            return List.of();
        }
    }

    // ─── Ürün bazlı öneri ("bunu alanlar bunu da aldı") ──────────────────────

    @Override
    public List<Long> getProductBasedRecommendations(Long productId, int limit) {
        int normLimit = normalizeLimit(limit, 6);
        try (Session session = driver.session()) {
            return session.run("""
                MATCH (:User)-[:PURCHASED]->(p:Product {productId: $pid})<-[:PURCHASED]-(u)
                      -[:PURCHASED]->(rec)
                WHERE rec.productId <> $pid
                RETURN rec.productId AS productId, count(u) AS score
                ORDER BY score DESC
                LIMIT $limit
                """, Values.parameters("pid", productId, "limit", normLimit))
                    .list(r -> r.get("productId").asLong());

        } catch (Exception e) {
            log.warn("Ürün önerisi alınamadı (productId={}): {}", productId, e.getMessage());
            return List.of();
        }
    }

    // ─── Popüler ürünler (fallback) ───────────────────────────────────────────

    @Override
    public List<Long> getPopularProducts(int limit) {
        int normLimit = normalizeLimit(limit, 8);
        try (Session session = driver.session()) {
            List<Long> result = session.run("""
                MATCH (:User)-[:PURCHASED]->(p:Product)
                RETURN p.productId AS productId, count(*) AS cnt
                ORDER BY cnt DESC
                LIMIT $limit
                """, Values.parameters("limit", normLimit))
                    .list(r -> r.get("productId").asLong());

            if (!result.isEmpty()) return result;

            // Hiç PURCHASED yoksa tüm ürünlerden ilklerini döndür
            return session.run("MATCH (p:Product) RETURN p.productId AS productId LIMIT $limit",
                            Values.parameters("limit", normLimit))
                    .list(r -> r.get("productId").asLong());

        } catch (Exception e) {
            log.warn("Popüler ürünler alınamadı: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private static int normalizeLimit(int limit, int defaultIfInvalid) {
        if (limit <= 0) {
            return Math.min(defaultIfInvalid, LIMIT_HARD_CAP);
        }
        return Math.min(Math.max(limit, 1), LIMIT_HARD_CAP);
    }
}
