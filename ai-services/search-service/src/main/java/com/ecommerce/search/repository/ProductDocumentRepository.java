package com.ecommerce.search.repository;

import com.ecommerce.search.entity.ProductDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductDocumentRepository extends JpaRepository<ProductDocument, Long> {
    Optional<ProductDocument> findByProductId(Long productId);

    List<ProductDocument> findByEmbeddingGeneratedFalseAndActiveTrue(Pageable pageable);

    List<ProductDocument> findByProductIdInAndActiveTrue(Collection<Long> productIds);
}
