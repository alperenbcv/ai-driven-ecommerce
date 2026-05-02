package com.ecommerce.payment.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PaymentInitiateEvent {
    private String orderId;
    private Long userId;
    private String amount;
    private String buyerName;
    private String buyerPhone;
    private List<Map<String, Object>> items;

    public static PaymentInitiateEvent from(Map<String, Object> map) {
        PaymentInitiateEvent event = new PaymentInitiateEvent();
        event.setOrderId(map.get("orderId").toString());
        event.setUserId(Long.valueOf(map.get("userId").toString()));
        event.setAmount(map.get("amount").toString());
        event.setBuyerName(map.getOrDefault("buyerName", "").toString());
        event.setBuyerPhone(map.getOrDefault("buyerPhone", "").toString());
        if (map.get("items") instanceof List<?> items) {
            event.setItems(items.stream()
                    .filter(i -> i instanceof Map)
                    .map(i -> (Map<String, Object>) i)
                    .toList());
        }
        return event;
    }
}
