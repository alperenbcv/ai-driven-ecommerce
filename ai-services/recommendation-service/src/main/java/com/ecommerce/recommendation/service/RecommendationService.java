package com.ecommerce.recommendation.service;

import java.util.List;
import java.util.Map;

/** Neo4j tabanlı üneri grafı ve sorguları — uygulama {@link RecommendationServiceImpl}. */
public interface RecommendationService {

    void recordPurchase(Map<String, Object> orderEvent);

    void recordView(Long userId, Long productId, String productName, String category);

    List<Long> getRecommendationsForUser(Long userId, int limit);

    List<Long> getProductBasedRecommendations(Long productId, int limit);

    List<Long> getPopularProducts(int limit);
}
