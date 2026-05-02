package com.ecommerce.search.service;

import com.ecommerce.search.entity.ProductDocument;
import com.ecommerce.search.repository.ProductDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * VectorStore yazımı uzun süren işlemdir; persistence katmanında kısa transaction,
 * embedding OpenAI çağrıları transaction DIŞINDA yapılır.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {

    private final VectorStore vectorStore;
    private final ProductDocumentRepository documentRepository;
    private final SearchCatalogPersistence catalogPersistence;

    private static final int EMBEDDING_PAGE = 50;
    private static final int QUERY_TOP_MIN = 1;
    private static final int QUERY_TOP_MAX = 50;

    @Override
    public void indexProduct(Map<String, Object> productEvent) {
        catalogPersistence.upsertFromProductEvent(productEvent);
    }

    @Override
    public void deactivateProduct(Long productId) {
        catalogPersistence.deactivate(productId);
        vectorStore.delete(List.of(productIdToUuid(productId)));
        log.info("Ürün arama index'inden çıkarıldı: {}", productId);
    }

    @Override
    public List<Long> search(String query, int topK, double minScore) {
        int k = clampTopK(topK);
        int fetchCap = Math.min(200, k * 5);

        List<Document> raw = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query.trim())
                        .topK(fetchCap)
                        .similarityThreshold(minScore)
                        .build()
        );

        List<Long> candidateOrder = extractProductIdsPreserveOrder(raw);
        if (candidateOrder.isEmpty()) {
            return List.of();
        }

        List<ProductDocument> activeRows = documentRepository.findByProductIdInAndActiveTrue(candidateOrder);
        Set<Long> active = activeRows.stream()
                .map(ProductDocument::getProductId)
                .collect(java.util.stream.Collectors.toSet());

        List<Long> filtered = candidateOrder.stream()
                .distinct()
                .filter(active::contains)
                .limit(k)
                .toList();

        return filtered;
    }

    /**
     * Her ürün id'si için vector store tek kayıt: önce deterministik UUID silinerek upsert garantilenir.
     */
    private void syncVectorEmbedding(Long productId) {
        ProductDocument doc = documentRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalStateException("İndeks belgesi yok: productId=" + productId));
        try {
            String uuid = productIdToUuid(productId);
            vectorStore.delete(List.of(uuid));

            String content = doc.toSearchableContent();
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("productId", doc.getProductId().toString());
            metadata.put("category", doc.getCategory() != null ? doc.getCategory() : "");
            metadata.put("brand", doc.getBrand() != null ? doc.getBrand() : "");
            metadata.put("minPrice", doc.getMinPrice() != null ? doc.getMinPrice().doubleValue() : 0.0);
            metadata.put("active", doc.isActive());

            Document document = new Document(uuid, content, metadata);
            vectorStore.add(List.of(document));

            catalogPersistence.markEmbeddingGenerated(productId, true);
            log.debug("Embedding senkronize edildi: productId={}, uuid={}", productId, uuid);
        } catch (Exception e) {
            log.error("Embedding üretimi başarısız: productId={}", productId, e);
            catalogPersistence.markEmbeddingGenerated(productId, false);
        }
    }

    /**
     * Kuyruk hızını düşürmeden batch: yalnızca DB güncellenir (embedding sonra); listener OpenAI'ya takılmaz.
     */
    @Scheduled(fixedDelay = 600000, initialDelay = 60000)
    public void generateMissingEmbeddings() {
        while (true) {
            List<ProductDocument> batch = documentRepository
                    .findByEmbeddingGeneratedFalseAndActiveTrue(PageRequest.of(0, EMBEDDING_PAGE));
            if (batch.isEmpty()) {
                return;
            }
            log.info("Embedding üretilecek ürün sayısı (batch): {}", batch.size());
            for (ProductDocument doc : batch) {
                try {
                    syncVectorEmbedding(doc.getProductId());
                } catch (Exception e) {
                    log.error("Embedding batch hatası: productId={}", doc.getProductId(), e);
                }
            }
        }
    }

    private static int clampTopK(int topK) {
        return Math.max(QUERY_TOP_MIN, Math.min(QUERY_TOP_MAX, topK));
    }

    private static List<Long> extractProductIdsPreserveOrder(List<Document> docs) {
        List<Long> out = new ArrayList<>();
        for (Document d : docs) {
            Long id = null;
            Object pidMeta = d.getMetadata().get("productId");
            if (pidMeta != null) {
                try {
                    id = Long.valueOf(pidMeta.toString());
                } catch (NumberFormatException ignored) {
                }
            }
            if (id == null) {
                String sid = d.getId();
                if (sid != null && sid.startsWith("00000000-0000-0000-0000-")) {
                    try {
                        id = Long.parseLong(sid.substring(sid.lastIndexOf('-') + 1));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            if (id != null) {
                out.add(id);
            }
        }
        return out;
    }

    private static String productIdToUuid(Long productId) {
        return String.format("00000000-0000-0000-0000-%012d", productId);
    }
}
