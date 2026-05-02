package com.ecommerce.stock.repository;

import com.ecommerce.stock.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByProductIdAndSellerIdIsNull(Long productId);

    Optional<Stock> findByProductIdAndSellerId(Long productId, Long sellerId);

    List<Stock> findByProductId(Long productId);

    boolean existsByProductIdAndSellerId(Long productId, Long sellerId);

    @Modifying
    @Query("""
        UPDATE Stock s
        SET s.reservedQty = s.reservedQty + :amount
        WHERE s.productId = :productId
          AND s.sellerId  = :sellerId
          AND (s.quantity - s.reservedQty) >= :amount
    """)
    int reserveStockForSeller(Long productId, Long sellerId, int amount);

    @Modifying
    @Query("""
        UPDATE Stock s
        SET s.reservedQty = s.reservedQty + :amount
        WHERE s.productId = :productId
          AND s.sellerId IS NULL
          AND (s.quantity - s.reservedQty) >= :amount
    """)
    int reserveStockLegacy(Long productId, int amount);

    @Modifying
    @Query("""
        UPDATE Stock s
        SET s.reservedQty = s.reservedQty - :amount
        WHERE s.productId = :productId
          AND s.sellerId  = :sellerId
          AND s.reservedQty >= :amount
    """)
    int releaseReservationForSeller(Long productId, Long sellerId, int amount);

    @Modifying
    @Query("""
        UPDATE Stock s
        SET s.reservedQty = s.reservedQty - :amount
        WHERE s.productId = :productId
          AND s.sellerId IS NULL
          AND s.reservedQty >= :amount
    """)
    int releaseReservationLegacy(Long productId, int amount);

    @Modifying
    @Query("""
        UPDATE Stock s
        SET s.quantity    = s.quantity    - :amount,
            s.reservedQty = s.reservedQty - :amount
        WHERE s.productId = :productId
          AND s.sellerId  = :sellerId
          AND s.reservedQty >= :amount
    """)
    int confirmReservationForSeller(Long productId, Long sellerId, int amount);

    @Modifying
    @Query("""
        UPDATE Stock s
        SET s.quantity    = s.quantity    - :amount,
            s.reservedQty = s.reservedQty - :amount
        WHERE s.productId = :productId
          AND s.sellerId IS NULL
          AND s.reservedQty >= :amount
    """)
    int confirmReservationLegacy(Long productId, int amount);
}
