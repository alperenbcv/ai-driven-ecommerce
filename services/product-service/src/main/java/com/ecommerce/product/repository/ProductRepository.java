package com.ecommerce.product.repository;

import com.ecommerce.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

/**
 * JpaSpecificationExecutor<Product> → dinamik filtreleme için.
 * findAll(Specification, Pageable) metodunu kullanmamızı sağlar.
 *
 * JpaRepository + JpaSpecificationExecutor birlikte kullanılır:
 * - JpaRepository: save, findById, findAll gibi temel CRUD
 * - JpaSpecificationExecutor: Specification nesnesiyle dinamik WHERE koşulları
 */
public interface ProductRepository extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {

    /**
     * Aktif ürünleri sayfalayarak listele.
     * Spring Data metot ismine bakıp SQL üretiyor:
     * SELECT * FROM products WHERE active = true
     */
    Page<Product> findByActiveTrue(Pageable pageable);

    /** ID listesi toplu doldurma (asistan önerisi vb.) — maksimum sayı serviste kısıtlanır */
    List<Product> findByIdInAndActiveTrue(Collection<Long> ids);

    /**
     * Embedding üretilmemiş aktif ürünleri bul.
     * Search Service başladığında geriye dönük işleme için.
     */
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.embeddingGenerated = false")
    java.util.List<Product> findProductsWithoutEmbedding();

    /**
     * Bir ürünün embedding durumunu güncelle.
     * @Modifying → SELECT değil UPDATE/DELETE sorgusu olduğunu belirtir.
     * @Transactional olmadan @Modifying çalışmaz — serviste sağlanıyor.
     */
    @Modifying
    @Query("UPDATE Product p SET p.embeddingGenerated = true WHERE p.id = :productId")
    void markEmbeddingGenerated(Long productId);
}
