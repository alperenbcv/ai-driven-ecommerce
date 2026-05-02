package com.ecommerce.payment.entity;

import com.ecommerce.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 *
 * iyzicoToken       Iyzico'dan dönen checkout token.
 * iyzicoPaymentId   Başarılı ödemelerde Iyzico'nun atadığı benzersiz ödeme ID'si.
 * conversationId    Bizim gönderdiğimiz correlation ID (orderNumber).
 * rawResponse      Iyzico'dan gelen ham JSON yanıt.
 */
@Entity
@Table(name = "payment_transactions", indexes = {
    @Index(name = "idx_payment_order", columnList = "order_number"),
    @Index(name = "idx_payment_token", columnList = "iyzico_token")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction extends BaseEntity {

    public enum PaymentStatus {
        PENDING, SUCCESS, FAILED, CANCELLED, REFUNDED
    }

    @Column(name = "order_number", nullable = false, length = 30)
    private String orderNumber;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "iyzico_token", length = 200)
    private String iyzicoToken;

    @Column(name = "iyzico_payment_id", length = 100)
    private String iyzicoPaymentId;

    @Column(name = "conversation_id", length = 50)
    private String conversationId;

    @Column(columnDefinition = "TEXT")
    private String rawResponse;

    @Column(length = 500)
    private String failureReason;
}
