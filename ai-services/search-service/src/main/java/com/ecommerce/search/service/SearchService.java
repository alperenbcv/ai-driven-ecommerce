package com.ecommerce.search.service;

import java.util.List;
import java.util.Map;

public interface SearchService {

    void indexProduct(Map<String, Object> productEvent);

    void deactivateProduct(Long productId);

    List<Long> search(String query, int topK, double minScore);
}
