package com.ecommerce.search.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Arama için ürün index'i — pgvector'de saklanır.
 *
 * Bu entity Product Service'teki Product entity'sinin "arama kopyası".
 * Product Service bir ürün oluşturduğunda veya güncellediğinde
 * RabbitMQ üzerinden "product.created" veya "product.updated" eventi gelir.
 * Search Service bu eventi alır ve ProductDocument'i günceller.
 *
 * embedding kolonunu neden String tutuyoruz?
 * pgvector'de vektörler float[] olarak saklanır.
 * Spring Data JPA'da native pgvector tip desteği için
 * Spring AI'ın PgVectorVectorStore'unu kullanacağız.
 * Bu entity doğrudan embedding aramamak için,
 * metadata araması (fiyat filtresi, kategori) için kullanılır.
 * Embedding araması → VectorStore.similaritySearch() ile yapılır.
 *
 * content → embedding üretilecek metin (name + description + category + brand)
 *            "Lacoste Erkek Sneaker Beyaz Spor Ayakkabı | Ayakkabı | Lacoste"
 * embeddingGenerated → embedding'in üretilip üretilmediği (batch job için)
 */
@Entity
@Table(name = "product_documents", indexes = {
    @Index(name = "idx_pdoc_product", columnList = "product_id", unique = true),
    @Index(name = "idx_pdoc_category", columnList = "category"),
    @Index(name = "idx_pdoc_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDocument {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @Column(nullable = false, length = 300)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String category;

    @Column(length = 100)
    private String brand;

    @Column(precision = 12, scale = 2)
    private BigDecimal minPrice;

    @Column(precision = 3, scale = 2)
    private BigDecimal averageRating;

    @Column
    private boolean active;

    @Column(name = "embedding_generated")
    private boolean embeddingGenerated;

    /**
     * VectorStore'a gönderilen metin.
     * Embedding bu metinden üretilir.
     * Daha zengin metin → daha iyi arama sonuçları.
     */
    public String toSearchableContent() {
        return String.join(" | ",
                name != null ? name : "",
                description != null ? description.substring(0, Math.min(description.length(), 500)) : "",
                category != null ? category : "",
                brand != null ? brand : ""
        );
    }

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() { this.createdAt = LocalDateTime.now(); this.updatedAt = LocalDateTime.now(); }
    @PreUpdate
    void onUpdate() { this.updatedAt = LocalDateTime.now(); }
}
