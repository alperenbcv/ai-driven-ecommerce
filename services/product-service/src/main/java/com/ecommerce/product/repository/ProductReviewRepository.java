package com.ecommerce.product.repository;

import com.ecommerce.product.entity.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    boolean existsByProductIdAndUserId(Long productId, Long userId);

    Page<ProductReview> findByProductIdAndApprovedTrueOrderByCreatedAtDesc(
            Long productId, Pageable pageable);

    Optional<ProductReview> findByProductIdAndUserId(Long productId, Long userId);
}
