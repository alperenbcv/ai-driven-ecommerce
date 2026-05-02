package com.ecommerce.recommendation.node;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Neo4j node: Kullanıcı.
 *
 * @Node → Bu sınıf bir Neo4j node'u temsil eder (label: "User")
 * @Id   → Neo4j node'larının benzersiz tanımlayıcısı
 *         JPA'daki @Id/@GeneratedValue yerine @Id @GeneratedValue(GenerationType.UUID)
 *         veya doğrudan domain ID kullanılır.
 *         Biz userId'yi (Long) doğrudan kullandık.
 *
 * @Relationship → Grafta edge (ilişki) tanımı.
 *   type: ilişkinin adı (Cypher'da [:PURCHASED] olarak görünür)
 *   direction: OUTGOING = (User) -[:PURCHASED]-> (Product)
 *
 * Neden bu graF yapısı?
 * Geleneksel DB'de "Collaborative Filtering" için büyük JOIN'lar gerekir.
 * Neo4j'da bu bir graph traversal:
 *   MATCH (u:User {userId:42})-[:PURCHASED]->(p:Product)<-[:PURCHASED]-(other:User)-[:PURCHASED]->(rec:Product)
 *   WHERE NOT (u)-[:PURCHASED]->(rec)
 *   RETURN rec, count(other) as score ORDER BY score DESC LIMIT 5
 * → "Seni gibi kullanıcılar bunları da aldı"
 */
@Node("User")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserNode {

    @Id
    private Long userId;

    private String name;

    @Relationship(type = "PURCHASED", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private Set<ProductNode> purchasedProducts = new HashSet<>();

    @Relationship(type = "VIEWED", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private Set<ProductNode> viewedProducts = new HashSet<>();
}
