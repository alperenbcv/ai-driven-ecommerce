/**
 * Spring Data Neo4j repository sözleşmeleri şu an {@link com.ecommerce.recommendation.service.RecommendationServiceImpl}
 * içinde kullanılmıyor — servis doğrudan {@link org.neo4j.driver.Driver} ile çalışıyor.
 * Graf entity'leri ile SDN'e geçiş için tutulmuştur.
 */
package com.ecommerce.recommendation.repository;
