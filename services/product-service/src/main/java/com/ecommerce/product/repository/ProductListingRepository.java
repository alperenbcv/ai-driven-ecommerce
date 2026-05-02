package com.ecommerce.product.repository;

import com.ecommerce.product.entity.ProductListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductListingRepository extends JpaRepository<ProductListing, Long> {

    List<ProductListing> findByProductIdAndActiveTrueOrderByPriceAsc(Long productId);

    List<ProductListing> findBySellerIdOrderByCreatedAtDesc(Long sellerId);

    Optional<ProductListing> findByProductIdAndSellerId(Long productId, Long sellerId);

    boolean existsByProductIdAndSellerId(Long productId, Long sellerId);

    /**
     * Bir ürünün en düşük fiyatlı aktif listing'ini bul.
     */
    @Query("SELECT l FROM ProductListing l WHERE l.product.id = :productId AND l.active = true ORDER BY l.price ASC LIMIT 1")
    Optional<ProductListing> findCheapestListing(Long productId);
}
