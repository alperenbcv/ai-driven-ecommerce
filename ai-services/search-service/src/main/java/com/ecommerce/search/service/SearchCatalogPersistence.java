package com.ecommerce.search.service;

import com.ecommerce.search.entity.ProductDocument;
import com.ecommerce.search.repository.ProductDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

/**
 * JPA yazıları kısa transaction'larda tutulur — OpenAI / vector işlemi burada yapılmaz.
 */
@Component
@RequiredArgsConstructor
public class SearchCatalogPersistence {

    private final ProductDocumentRepository documentRepository;

    @Transactional
    public ProductDocument upsertFromProductEvent(Map<String, Object> productEvent) {
        Long productId = Long.valueOf(productEvent.get("productId").toString());
        String name = productEvent.getOrDefault("name", "").toString();
        String description = productEvent.getOrDefault("description", "").toString();
        String category = "";
        Object c = productEvent.get("category");
        Object cn = productEvent.get("categoryName");
        if (c != null && !c.toString().isBlank()) {
            category = c.toString();
        } else if (cn != null) {
            category = cn.toString();
        }

        String brand = productEvent.getOrDefault("brand", "").toString();
        BigDecimal minPrice = productEvent.get("price") != null
                ? new BigDecimal(productEvent.get("price").toString())
                : BigDecimal.ZERO;

        ProductDocument doc = documentRepository.findByProductId(productId)
                .orElse(ProductDocument.builder().productId(productId).build());

        doc.setName(name);
        doc.setDescription(description);
        doc.setCategory(category);
        doc.setBrand(brand);
        doc.setMinPrice(minPrice);
        doc.setActive(readActive(productEvent));
        doc.setEmbeddingGenerated(false);

        return documentRepository.save(doc);
    }

    private static boolean readActive(Map<String, Object> productEvent) {
        Object a = productEvent.get("active");
        if (a == null) {
            return true;
        }
        if (a instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(a.toString());
    }

    @Transactional
    public void markEmbeddingGenerated(Long productId, boolean generated) {
        documentRepository.findByProductId(productId).ifPresent(d -> {
            d.setEmbeddingGenerated(generated);
            documentRepository.save(d);
        });
    }

    @Transactional
    public void deactivate(Long productId) {
        documentRepository.findByProductId(productId).ifPresent(doc -> {
            doc.setActive(false);
            documentRepository.save(doc);
        });
    }
}
