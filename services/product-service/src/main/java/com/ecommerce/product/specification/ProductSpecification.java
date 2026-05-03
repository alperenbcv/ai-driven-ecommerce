package com.ecommerce.product.specification;

import com.ecommerce.product.entity.Brand;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.entity.Product;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

/**
 *
 * Spring Data JPA Specification, runtime'da WHERE koşullarını parçalı şekilde
 * oluşturmayı sağlar. Böylece her filtre kombinasyonu için ayrı repository metodu
 * yazmak yerine küçük ve tekrar kullanılabilir koşullar tanımlanır.
 *
 * Örnek kullanım:
 * Specification<Product> spec = Specification.where(ProductSpecification.isActive());
 * spec = spec.and(ProductSpecification.nameContains(keyword));
 * spec = spec.and(ProductSpecification.inCategory(categoryId));
 *
 * Bu yapı özellikle ürün listeleme ekranında kullanışlıdır çünkü kullanıcı aynı anda:
 * - ürün adı / açıklama / kategori / marka araması,
 * - kategori filtresi,
 * - marka filtresi,
 * - minimum ve maksimum fiyat filtresi
 * uygulayabilir.
 *
 * Metotların her biri tek bir filtre koşulu döndürür.
 * Service katmanı bu koşulları .and() veya .or() ile birleştirerek final sorguyu oluşturur.
 *
 * private constructor kullanılmasının sebebi zaten tüm metodlar static, nesne oluşturulmasına gerek yok.
 */

public class ProductSpecification {

    private ProductSpecification() {}
    
    public static Specification<Product> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    public static Specification<Product> nameContains(String keyword) {
        String pattern = "%" + keyword.toLowerCase() + "%";
        return (root, query, cb) -> {
            query.distinct(true);
            Join<Product, Category> categoryJoin = root.join("category", JoinType.LEFT);
            Join<Product, Brand>    brandJoin    = root.join("brand",    JoinType.LEFT);
            return cb.or(
                cb.like(cb.lower(root.get("name")),         pattern),
                cb.like(cb.lower(categoryJoin.get("name")), pattern),
                cb.like(cb.lower(brandJoin.get("name")),    pattern),
                cb.like(cb.lower(root.get("description")),  pattern)
            );
        };
    }

    public static Specification<Product> inCategory(Long categoryId) {
        return (root, query, cb) -> cb.or(
            cb.equal(root.get("category").get("id"), categoryId),
            cb.equal(root.get("category").get("parent").get("id"), categoryId)
        );
    }

    public static Specification<Product> fromBrand(Long brandId) {
        return (root, query, cb) ->
            cb.equal(root.get("brand").get("id"), brandId);
    }

    public static Specification<Product> priceGreaterThanOrEqual(BigDecimal minPrice) {
        return (root, query, cb) ->
            cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Product> priceLessThanOrEqual(BigDecimal maxPrice) {
        return (root, query, cb) ->
            cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }
}
