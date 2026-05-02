package com.ecommerce.notification.listener;

import com.ecommerce.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final EmailService emailService;

    // ─── Sipariş eventleri ────────────────────────────────────────────────────

    @RabbitListener(queues = "notify.order.created.queue")
    public void onOrderCreated(Map<String, Object> event) {
        String email = str(event, "userEmail");
        String orderNumber = orderDisplayRef(event);
        String amount = firstNonBlank(
                str(event, "totalAmount"),
                formatAmount(event.get("totalAmount")),
                "—");
        if (email.isBlank()) {
            log.warn("Sipariş maili için email yok: {}", orderNumber);
            return;
        }

        emailService.sendHtml(email, "Siparişiniz Alındı — " + orderNumber, "order-created", Map.of(
                "firstName", "Değerli Müşterimiz",
                "orderNumber", orderNumber,
                "totalAmount", amount
        ));
    }

    @RabbitListener(queues = "notify.order.shipped.queue")
    public void onOrderShipped(Map<String, Object> event) {
        String email = str(event, "userEmail");
        String orderNumber = orderDisplayRef(event);
        String trackingNumber = str(event, "trackingNumber");
        String provider = str(event, "provider", "Kargo Firması");
        if (email.isBlank()) {
            return;
        }

        emailService.sendHtml(email, "Siparişiniz Kargoya Verildi — " + orderNumber, "order-shipped", Map.of(
                "firstName", "Değerli Müşterimiz",
                "orderNumber", orderNumber,
                "provider", provider,
                "trackingNumber", trackingNumber
        ));
    }

    @RabbitListener(queues = "notify.order.delivered.queue")
    public void onOrderDelivered(Map<String, Object> event) {
        String email = str(event, "userEmail");
        String orderNumber = orderDisplayRef(event);
        if (email.isBlank()) {
            return;
        }

        emailService.sendHtml(email, "Siparişiniz Teslim Edildi — " + orderNumber, "order-delivered", Map.of(
                "firstName", "Değerli Müşterimiz",
                "orderNumber", orderNumber
        ));
    }

    @RabbitListener(queues = "notify.order.cancelled.queue")
    public void onOrderCancelled(Map<String, Object> event) {
        String email = str(event, "userEmail");
        String orderNumber = orderDisplayRef(event);
        String reason = firstNonBlank(
                str(event, "cancelReason"),
                str(event, "reason"),
                "Sipariş iptal edildi");
        if (email.isBlank()) {
            return;
        }

        emailService.sendHtml(email, "Siparişiniz İptal Edildi — " + orderNumber, "order-cancelled", Map.of(
                "firstName", "Değerli Müşterimiz",
                "orderNumber", orderNumber,
                "reason", reason
        ));
    }

    @RabbitListener(queues = "notify.payment.failed.queue")
    public void onPaymentFailed(Map<String, Object> event) {
        String email = firstNonBlank(str(event, "userEmail"), str(event, "buyerEmail"));
        String orderNumber = orderDisplayRef(event);
        String reason = firstNonBlank(
                str(event, "failureReason"),
                str(event, "reason"),
                "Ödeme işlemi başarısız");
        if (email.isBlank()) {
            return;
        }

        emailService.sendHtml(email, "Ödeme Başarısız — " + orderNumber, "payment-failed", Map.of(
                "firstName", "Değerli Müşterimiz",
                "orderNumber", orderNumber,
                "reason", reason
        ));
    }

    @RabbitListener(queues = "notify.stock.low.queue")
    public void onLowStock(Map<String, Object> event) {
        String productId = str(event, "productId", "?");
        String productName = firstNonBlank(
                str(event, "productName"),
                str(event, "name"),
                "Bilinmiyor");
        String quantity = firstNonBlank(
                str(event, "availableQty"),
                str(event, "availableQuantity"),
                str(event, "quantity"),
                str(event, "stock"),
                "?");

        emailService.sendSimple(
                "admin@ecommerce.com",
                "[UYARI] Düşük Stok: " + productName,
                "Ürün: %s (ID: %s)\nMevcut stok: %s adet\n\nLütfen stok durumunu kontrol edin."
                        .formatted(productName, productId, quantity)
        );
    }

    // ─── Kullanıcı eventleri ─────────────────────────────────────────────────

    @RabbitListener(queues = "notify.user.registered.queue")
    public void onUserRegistered(Map<String, Object> event) {
        String email = firstNonBlank(str(event, "email"), str(event, "userEmail"));
        String firstName = str(event, "firstName", "Değerli Üyemiz");
        String verificationLink = str(event, "verificationLink");
        if (email.isBlank()) {
            return;
        }

        emailService.sendHtml(email, "E-posta Adresinizi Doğrulayın", "email-verification", Map.of(
                "firstName", firstName,
                "verificationLink", verificationLink
        ));
        log.info("Doğrulama maili gönderildi: {}", email);
    }

    @RabbitListener(queues = "notify.user.password-reset.queue")
    public void onPasswordReset(Map<String, Object> event) {
        String email = firstNonBlank(str(event, "email"), str(event, "userEmail"));
        String firstName = str(event, "firstName", "Değerli Üyemiz");
        String resetLink = str(event, "resetLink");
        if (email.isBlank()) {
            return;
        }

        emailService.sendHtml(email, "Şifre Sıfırlama Talebi", "password-reset", Map.of(
                "firstName", firstName,
                "resetLink", resetLink
        ));
        log.info("Şifre sıfırlama maili gönderildi: {}", email);
    }

    /** Order service notification payload: {@code orderId} = kullanıcıya görünen numara (ORD-...); ayrıca {@code orderNumber} gelebilir. */
    private String orderDisplayRef(Map<String, Object> event) {
        return firstNonBlank(str(event, "orderNumber"), str(event, "orderId"));
    }

    /** totalAmount bazen JSON'da sayı olarak gelir; şablonda düzgün göstermek için normalize edilir. */
    private String formatAmount(Object totalAmount) {
        if (totalAmount == null) {
            return "";
        }
        try {
            return new BigDecimal(totalAmount.toString()).toPlainString();
        } catch (Exception e) {
            return totalAmount.toString();
        }
    }

    private static String firstNonBlank(String... candidates) {
        for (String v : candidates) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return "";
    }

    private String str(Map<String, Object> map, String key) {
        return str(map, key, "");
    }

    private String str(Map<String, Object> map, String key, String defaultVal) {
        Object val = map.get(key);
        return val != null ? val.toString() : defaultVal;
    }
}
