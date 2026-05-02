package com.ecommerce.recommendation.repository;

import com.ecommerce.recommendation.node.UserNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Neo4j Repository — Cypher sorguları.
 *
 * Neo4jRepository → JPA'daki JpaRepository gibi ama Neo4j için.
 * @Query → Cypher sorgusu (SQL yerine graph traversal dili).
 *
 * Cypher sözdizimi:
 *   (node:Label {property:value})   → node tanımı
 *   -[:RELATIONSHIP]->              → yönlü edge
 *   MATCH                           → pattern ara
 *   WHERE NOT                       → filtrele
 *   RETURN x, count(y)              → sonuç
 *   ORDER BY ... DESC LIMIT n       → sırala + sınırla
 */
public interface UserNodeRepository extends Neo4jRepository<UserNode, Long> {

    Optional<UserNode> findByUserId(Long userId);

    /**
     * Collaborative Filtering — "Bunu satın alanlar bunu da aldı"
     *
     * Cypher akışı:
     * 1. (u) → userId'ye göre kullanıcı bul
     * 2. (u)-[:PURCHASED]->(p) → bu kullanıcının aldığı ürünler
     * 3. <-[:PURCHASED]-(other) → bu ürünü alan diğer kullanıcılar
     * 4. (other)-[:PURCHASED]->(rec) → diğer kullanıcıların aldığı ürünler
     * 5. WHERE NOT (u)-[:PURCHASED]->(rec) → kullanıcının henüz almadıkları
     * 6. count(other) → kaç farklı kullanıcı ortak → skorla
     */
    @Query("""
        MATCH (u:User {userId: $userId})-[:PURCHASED]->(p:Product)<-[:PURCHASED]-(other:User)-[:PURCHASED]->(rec:Product)
        WHERE NOT (u)-[:PURCHASED]->(rec)
        RETURN rec.productId AS productId, count(other) AS score
        ORDER BY score DESC
        LIMIT $limit
        """)
    List<RecommendationResult> findCollaborativeRecommendations(
            @Param("userId") Long userId,
            @Param("limit") int limit);

    /**
     * Content-Based Filtering — Aynı kategoriden öneriler.
     * Kullanıcının aldığı ürünlerin kategorilerindeki diğer ürünleri önerir.
     */
    @Query("""
        MATCH (u:User {userId: $userId})-[:PURCHASED]->(p:Product)
        MATCH (rec:Product {category: p.category})
        WHERE NOT (u)-[:PURCHASED]->(rec) AND rec.productId <> p.productId
        RETURN rec.productId AS productId, count(p) AS score
        ORDER BY score DESC
        LIMIT $limit
        """)
    List<RecommendationResult> findCategoryBasedRecommendations(
            @Param("userId") Long userId,
            @Param("limit") int limit);

    /**
     * "Bu ürünü alanlar bunu da aldı" — ürün bazlı öneri.
     */
    @Query("""
        MATCH (p:Product {productId: $productId})<-[:PURCHASED]-(u:User)-[:PURCHASED]->(rec:Product)
        WHERE rec.productId <> $productId
        RETURN rec.productId AS productId, count(u) AS score
        ORDER BY score DESC
        LIMIT $limit
        """)
    List<RecommendationResult> findProductBasedRecommendations(
            @Param("productId") Long productId,
            @Param("limit") int limit);

    interface RecommendationResult {
        Long getProductId();
        Long getScore();
    }
}
