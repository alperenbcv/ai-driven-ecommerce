package com.ecommerce.order.repository;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);

    @Modifying
    @Query("UPDATE Order o SET o.status = :newStatus WHERE o.orderNumber = :orderNumber AND o.status = :expectedStatus")
    int updateStatus(String orderNumber, OrderStatus expectedStatus, OrderStatus newStatus);

    @Modifying
    @Query("UPDATE Order o SET o.status = :status, o.cancelReason = :reason WHERE o.orderNumber = :orderNumber")
    int cancelOrder(String orderNumber, OrderStatus status, String reason);

    @Modifying
    @Query("UPDATE Order o SET o.paymentIntentId = :paymentIntentId, o.status = :status WHERE o.orderNumber = :orderNumber")
    int setPaymentIntent(String orderNumber, String paymentIntentId, OrderStatus status);

    @Modifying
    @Query("""
        UPDATE Order o SET o.cargoTrackingNumber = :trackingNumber,
        o.cargoProvider = :provider, o.status = :status, o.shippedAt = CURRENT_TIMESTAMP
        WHERE o.orderNumber = :orderNumber
    """)
    int setCargoInfo(String orderNumber, String trackingNumber, String provider, OrderStatus status);

    @Modifying
    @Query("UPDATE Order o SET o.status = :status, o.deliveredAt = CURRENT_TIMESTAMP WHERE o.orderNumber = :orderNumber AND o.status = :expectedStatus")
    int setDelivered(String orderNumber, OrderStatus expectedStatus, OrderStatus status);

    @Query("SELECT MAX(CAST(SUBSTRING(o.orderNumber, 16) AS long)) FROM Order o")
    Optional<Long> findMaxOrderSequence();
}
