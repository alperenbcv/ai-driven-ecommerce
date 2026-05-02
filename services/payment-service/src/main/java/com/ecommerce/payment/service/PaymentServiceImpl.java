package com.ecommerce.payment.service;

import com.ecommerce.common.exception.NotFoundException;
import com.ecommerce.payment.config.RabbitMQConfig;
import com.ecommerce.payment.dto.PaymentInitiateEvent;
import com.ecommerce.payment.entity.PaymentTransaction;
import com.ecommerce.payment.entity.PaymentTransaction.PaymentStatus;
import com.ecommerce.payment.repository.PaymentTransactionRepository;
import com.iyzipay.Options;
import com.iyzipay.model.*;
import com.iyzipay.request.CreateCheckoutFormInitializeRequest;
import com.iyzipay.request.RetrieveCheckoutFormRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 *
 * 1. Ödeme başlatma:
 *    initiatePayment() metodu Order Service'ten gelen PaymentInitiateEvent'i alır.
 *    Sipariş tutarı, kullanıcı bilgisi ve ürün kalemleriyle Iyzico Checkout Form
 *    isteği oluşturur. Iyzico başarılı cevap dönerse token ve checkout form HTML'i
 *    PaymentTransaction olarak veritabanına kaydedilir.
 *
 * 2. Iyzico callback doğrulama:
 *    handleCallback() metodu Iyzico'nun gönderdiği token ile ödeme sonucunu tekrar
 *    Iyzico üzerinden sorgular.
 *
 * 3. Saga event yayınlama:
 *    Ödeme başarılı olursa payment.success, başarısız olursa payment.failed eventi
 *    RabbitMQ üzerinden yayınlanır. Böylece Order Service ödeme sonucuna göre
 *    sipariş durumunu güncelleyebilir.
 *
 * 4. simulateSuccess() metodu local geliştirme için eklenmiştir, iyzico callback'i
 *    localhost'ta kullanılamadığı için şimdilik kullanılıyor.
 */

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private final PaymentTransactionRepository transactionRepository;
    private final RabbitTemplate rabbitTemplate;
    private final Options iyzicoOptions;

    @Value("${iyzico.callback-url}")
    private String callbackUrl;

    @Override
    @Transactional
    public String initiatePayment(PaymentInitiateEvent event) {
        BigDecimal amount = new BigDecimal(event.getAmount());

        PaymentTransaction transaction = PaymentTransaction.builder()
                .orderNumber(event.getOrderId())
                .userId(event.getUserId())
                .amount(amount)
                .conversationId(event.getOrderId())
                .build();

        CreateCheckoutFormInitializeRequest request = new CreateCheckoutFormInitializeRequest();
        request.setLocale(Locale.TR.getValue());
        request.setConversationId(event.getOrderId());
        request.setPrice(amount);
        request.setPaidPrice(amount);
        request.setCurrency(Currency.TRY.name());
        request.setEnabledInstallments(List.of(1, 2, 3, 6, 9));
        request.setPaymentGroup(PaymentGroup.PRODUCT.name());
        request.setCallbackUrl(callbackUrl);

        Buyer buyer = new Buyer();
        buyer.setId(event.getUserId().toString());
        buyer.setName(extractFirstName(event.getBuyerName()));
        buyer.setSurname(extractLastName(event.getBuyerName()));
        buyer.setGsmNumber(sanitizePhone(event.getBuyerPhone()));
        buyer.setEmail("user" + event.getUserId() + "@ecommerce.com");
        buyer.setIdentityNumber("74300864791");
        buyer.setRegistrationAddress("Türkiye");
        buyer.setIp("85.34.78.112");
        buyer.setCity("Istanbul");
        buyer.setCountry("Turkey");
        request.setBuyer(buyer);

        Address address = new Address();
        address.setContactName(event.getBuyerName());
        address.setCity("Istanbul");
        address.setCountry("Turkey");
        address.setAddress("Türkiye");
        request.setShippingAddress(address);
        request.setBillingAddress(address);

        List<BasketItem> basketItems = new ArrayList<>();
        if (event.getItems() != null) {
            event.getItems().forEach(item -> {
                BasketItem basketItem = new BasketItem();
                basketItem.setId("ITEM-" + item.get("name").toString().hashCode());
                basketItem.setName(item.get("name").toString());
                basketItem.setCategory1("Genel");
                basketItem.setItemType(BasketItemType.PHYSICAL.name());
                basketItem.setPrice(new BigDecimal(item.get("price").toString())
                        .multiply(BigDecimal.valueOf(Long.parseLong(item.get("quantity").toString()))));
                basketItems.add(basketItem);
            });
        } else {
            BasketItem item = new BasketItem();
            item.setId("ITEM-1");
            item.setName("Sipariş " + event.getOrderId());
            item.setCategory1("Genel");
            item.setItemType(BasketItemType.PHYSICAL.name());
            item.setPrice(amount);
            basketItems.add(item);
        }
        request.setBasketItems(basketItems);

        try {
            CheckoutFormInitialize result = CheckoutFormInitialize.create(request, iyzicoOptions);

            if ("success".equals(result.getStatus())) {
                transaction.setIyzicoToken(result.getToken());
                transaction.setRawResponse(result.getCheckoutFormContent());
                transactionRepository.save(transaction);

                log.info("Iyzico ödeme formu oluşturuldu: orderNumber={}, token={}",
                        event.getOrderId(), result.getToken());
                return result.getToken();
            } else {
                log.error("Iyzico form oluşturma başarısız: {}", result.getErrorMessage());
                transaction.setStatus(PaymentStatus.FAILED);
                transaction.setFailureReason(result.getErrorMessage());
                transactionRepository.save(transaction);

                publishPaymentFailed(event.getOrderId(), result.getErrorMessage());
                return null;
            }
        } catch (Exception e) {
            log.error("Iyzico bağlantı hatası: {}", e.getMessage());
            transaction.setStatus(PaymentStatus.FAILED);
            transaction.setFailureReason("Iyzico bağlantı hatası");
            transactionRepository.save(transaction);
            publishPaymentFailed(event.getOrderId(), "Ödeme sistemi geçici olarak kullanılamıyor");
            return null;
        }
    }

    @Override
    @Transactional
    public void handleCallback(String token, String callbackStatus) {
        PaymentTransaction transaction = transactionRepository.findByIyzicoToken(token)
                .orElseThrow(() -> new NotFoundException("Token'a ait işlem bulunamadı: " + token));

        RetrieveCheckoutFormRequest retrieveRequest = new RetrieveCheckoutFormRequest();
        retrieveRequest.setLocale(Locale.TR.getValue());
        retrieveRequest.setConversationId(transaction.getConversationId());
        retrieveRequest.setToken(token);

        try {
            CheckoutForm result = CheckoutForm.retrieve(retrieveRequest, iyzicoOptions);
            transaction.setRawResponse(result.toString());

            if ("success".equals(result.getStatus()) && "success".equals(result.getPaymentStatus())) {
                transaction.setStatus(PaymentStatus.SUCCESS);
                transaction.setIyzicoPaymentId(result.getPaymentId());
                transactionRepository.save(transaction);

                log.info("Ödeme başarılı: orderNumber={}, paymentId={}",
                        transaction.getOrderNumber(), result.getPaymentId());
                publishPaymentSuccess(transaction.getOrderNumber(), result.getPaymentId());

            } else {
                String reason = result.getErrorMessage() != null ? result.getErrorMessage() : "Ödeme reddedildi";
                transaction.setStatus(PaymentStatus.FAILED);
                transaction.setFailureReason(reason);
                transactionRepository.save(transaction);

                log.warn("Ödeme başarısız: orderNumber={}, sebep={}", transaction.getOrderNumber(), reason);
                publishPaymentFailed(transaction.getOrderNumber(), reason);
            }
        } catch (Exception e) {
            log.error("Iyzico doğrulama hatası: {}", e.getMessage());
            transaction.setStatus(PaymentStatus.FAILED);
            transaction.setFailureReason("Doğrulama hatası");
            transactionRepository.save(transaction);
            publishPaymentFailed(transaction.getOrderNumber(), "Ödeme doğrulanamadı");
        }
    }

    @Override
    public PaymentTransaction getByOrderNumber(String orderNumber) {
        return transactionRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new NotFoundException("Ödeme işlemi bulunamadı: " + orderNumber));
    }

    private void publishPaymentSuccess(String orderNumber, String paymentId) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYMENT_EXCHANGE,
                RabbitMQConfig.PAYMENT_SUCCESS_KEY,
                Map.of("orderId", orderNumber, "paymentIntentId", paymentId, "status", "SUCCESS")
            );
        } catch (Exception e) {
            log.error("payment.success eventi gönderilemedi: {}", e.getMessage());
        }
    }

    private void publishPaymentFailed(String orderNumber, String reason) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYMENT_EXCHANGE,
                RabbitMQConfig.PAYMENT_FAILED_KEY,
                Map.of("orderId", orderNumber, "reason", reason, "status", "FAILED")
            );
        } catch (Exception e) {
            log.error("payment.failed eventi gönderilemedi: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void simulateSuccess(String orderNumber) {
        log.warn("[SANDBOX] payment.success eventi gönderiliyor: {}", orderNumber);
        String sandboxPaymentId = "SANDBOX-" + System.currentTimeMillis();

        // Mevcut transaction varsa güncelle, yoksa sadece event gönder
        transactionRepository.findByOrderNumber(orderNumber).ifPresent(tx -> {
            tx.setStatus(PaymentStatus.SUCCESS);
            tx.setIyzicoPaymentId(sandboxPaymentId);
            transactionRepository.save(tx);
        });

        publishPaymentSuccess(orderNumber, sandboxPaymentId);
    }

    private String extractFirstName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "Misafir";
        String[] parts = fullName.trim().split(" ");
        return parts[0];
    }

    private String extractLastName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "Kullanici";
        String[] parts = fullName.trim().split(" ");
        return parts.length > 1 ? parts[parts.length - 1] : "Kullanici";
    }

    private String sanitizePhone(String phone) {
        if (phone == null) return "+905000000000";
        return phone.startsWith("+") ? phone : "+90" + phone.replaceAll("[^0-9]", "");
    }
}
