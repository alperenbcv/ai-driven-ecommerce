package com.ecommerce.payment.service;

import com.ecommerce.payment.dto.PaymentInitiateEvent;
import com.ecommerce.payment.entity.PaymentTransaction;

public interface PaymentService {

    String initiatePayment(PaymentInitiateEvent event);

    void handleCallback(String token, String status);

    PaymentTransaction getByOrderNumber(String orderNumber);
    
    void simulateSuccess(String orderNumber);
}
