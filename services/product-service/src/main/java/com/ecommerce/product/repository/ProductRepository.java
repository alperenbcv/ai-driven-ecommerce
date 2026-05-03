package com.ecommerce.product.repository;

import com.ecommerce.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {

    Page<Product> findByActiveTrue(Pageable pageable);

    List<Product> findByIdInAndActiveTrue(Collection<Long> ids);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.embeddingGenerated = false")
    java.util.List<Product> findProductsWithoutEmbedding();

    @Modifying
    @Query("UPDATE Product p SET p.embeddingGenerated = true WHERE p.id = :productId")
    void markEmbeddingGenerated(Long productId);
}
