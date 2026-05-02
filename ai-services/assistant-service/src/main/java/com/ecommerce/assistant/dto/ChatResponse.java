package com.ecommerce.assistant.dto;

import com.ecommerce.assistant.dto.tool.ProductSummary;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatResponse {
    private String sessionId;
    private String reply;
    private String model;
    /** Tool araması ürün döndürdüğünde dolu gelir; frontend card olarak render eder.
     * Amaç frontend'in ürünleri kart olarak gösterebilmesi.
     */
    private List<ProductSummary> products;
}
