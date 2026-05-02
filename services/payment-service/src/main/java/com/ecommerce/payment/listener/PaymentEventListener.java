package com.ecommerce.payment.listener;

import com.ecommerce.payment.dto.PaymentInitiateEvent;
import com.ecommerce.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final PaymentService paymentService;

    @RabbitListener(queues = "payment.initiate.queue")
    public void onPaymentInitiate(Map<String, Object> rawEvent) {
        log.info("Ödeme başlatma isteği alındı: {}", rawEvent.get("orderId"));
        try {
            PaymentInitiateEvent event = PaymentInitiateEvent.from(rawEvent);
            paymentService.initiatePayment(event);
        } catch (Exception e) {
            log.error("Ödeme başlatma işlenemedi: {}", e.getMessage());
        }
    }
}
