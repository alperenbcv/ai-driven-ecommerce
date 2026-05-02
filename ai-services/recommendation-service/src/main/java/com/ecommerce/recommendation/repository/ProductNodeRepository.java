package com.ecommerce.recommendation.repository;

import com.ecommerce.recommendation.node.ProductNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.Optional;

public interface ProductNodeRepository extends Neo4jRepository<ProductNode, Long> {
    Optional<ProductNode> findByProductId(Long productId);
}
