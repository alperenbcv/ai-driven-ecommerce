package com.ecommerce.recommendation.node;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import lombok.*;

/**
 * Neo4j node: Ürün.
 *
 * Bu node sadece metadata tutar (id, name, category).
 * Detaylı bilgi Product Service'ten gelir.
 * Grafın amacı ilişkileri modellemek, ürün detayı değil.
 */
@Node("Product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductNode {

    @Id
    private Long productId;

    private String name;
    private String category;
}
