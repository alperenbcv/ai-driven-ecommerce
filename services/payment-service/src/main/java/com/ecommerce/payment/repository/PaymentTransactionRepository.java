package com.ecommerce.payment.repository;

import com.ecommerce.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByOrderNumber(String orderNumber);

    Optional<PaymentTransaction> findByIyzicoToken(String iyzicoToken);

    Optional<PaymentTransaction> findByConversationId(String conversationId);
}
