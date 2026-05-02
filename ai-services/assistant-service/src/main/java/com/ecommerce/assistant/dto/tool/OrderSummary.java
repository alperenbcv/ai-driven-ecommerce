package com.ecommerce.assistant.dto.tool;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


/**
 *
 * @JsonIgnoreProperties(ignoreUnknown = true)
 *   Jackson JSON deserialize ederken DTO'da olmayan alanları yok sayar.
 *  Order Service'den gelen response'da bizim DTO'da olmayan alanlar varsa
 *  Jackson hata atmak yerine görmezden gelir, geliştirme aşamasında çok fazla değişiklik
 *  yaptığım için bu şekilde bir kullanım tercih ettim.
 *
 */

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderSummary {
    private String orderNumber;
    private String status;
    private BigDecimal totalAmount;
    private String shippingFullName;
    private String shippingCity;
    private String cargoTrackingNumber;
    private String cargoProvider;
    private String cancelReason;
    private List<OrderItemSummary> items;
    private LocalDateTime createdAt;
    private LocalDateTime shippedAt;



/**
     *
     * Neden inner class yaptım;
     *
     * OrderItemSummary bu projede tek başına bağımsız bir DTO olarak değil,
     * OrderSummary içindeki items listesinin elemanı olarak anlamlıdır.
     *
     * Neden static yaptım;
     *
     * Inner class static tanımlandığında dış sınıfın instance'ına ihtiyaç duymaz.
     * Yani OrderItemSummary oluşturmak için önce OrderSummary nesnesi oluşturmak gerekmez.
     */

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderItemSummary {
        private Long productId;
        private String productName;
        private int quantity;
    }
}
