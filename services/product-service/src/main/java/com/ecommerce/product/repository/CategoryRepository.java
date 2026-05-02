package com.ecommerce.product.repository;

import com.ecommerce.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByNameIgnoreCase(String name);

    /**
     * Sadece üst seviye kategorileri getir (parent'ı olmayanlar).
     * Frontend'de kategori menüsü oluşturmak için kullanılır.
     */
    @Query("SELECT c FROM Category c WHERE c.parent IS NULL AND c.active = true")
    List<Category> findRootCategories();

    List<Category> findByParentIdAndActiveTrue(Long parentId);
}
