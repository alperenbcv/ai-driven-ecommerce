package com.ecommerce.product.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.NotFoundException;
import com.ecommerce.product.dto.request.ProductFilterRequest;
import com.ecommerce.product.dto.request.ProductRequest;
import com.ecommerce.product.dto.response.ProductBriefResponse;
import com.ecommerce.product.dto.response.ProductResponse;
import com.ecommerce.product.entity.Brand;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.ProductImage;
import com.ecommerce.product.mapper.ProductMapper;
import com.ecommerce.product.repository.BrandRepository;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.messaging.ProductEventPublisher;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.security.ProductImageAccess;
import com.ecommerce.product.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 *
 *
 * 1. Katalog ürünü yönetimi
 *    - createProduct(): Admin tarafından yeni katalog ürünü oluşturur.
 *    - updateProduct(): Var olan aktif ürünü günceller.
 *    - deleteProduct(): Ürünü fiziksel olarak silmez, active=false yaparak soft delete uygular.
 *
 * 2. Entity doğrulama
 *    Ürün oluşturma/güncelleme sırasında categoryId ve brandId gerçek kayıtlarla eşleştirilir.
 *
 * 3. Event yayınlama
 *    Ürün oluşturulduğunda, güncellendiğinde veya silindiğinde ProductEventPublisher
 *    üzerinden ilgili event yayınlanır.
 *
 *    Bunun amacı Product Service ile Search-Recommendation gibi servisleri doğrudan
 *    birbirine bağımlı hale getirmemektir. Ürün değişikliği olduğunda diğer servisler
 *    bu eventi dinleyerek embedding, index veya cache güncellemesi yapabilir.
 *
 * 4. Dinamik ürün filtreleme
 *    listProducts metodu ProductSpecification kullanarak runtime'da dinamik sorgu üretir.
 *
 *    Kullanıcı keyword, categoryId, brandId, minPrice, maxPrice gibi alanlardan
 *    istediğini gönderebilir. Null veya boş gelen filtreler sorguya eklenmez.
 *    Böylece tek endpoint ile çok farklı arama senaryoları desteklenir.
 *
 *    Specification yapısı sayesinde:
 *    - aktif ürün kontrolü,
 *    - isim/açıklama/kategori/marka araması,
 *    - kategori filtresi,
 *    - marka filtresi,
 *    - fiyat aralığı filtresi
 *
 *    zincirleme şekilde tek bir JPA sorgusuna dönüştürülür.
 *
 *    PageRequest ve Sort kullanılarak sayfalama ve sıralama da aynı sorguya eklenir.
 *
 * 5. Batch ürün özeti çekme
 *    batchActiveSummaries metodu özellikle ai servislerinin
 *    birden fazla productId için kısa ürün bilgisi istemesi amacıyla kullanılır.
 *
 * 6. Ürün görseli yükleme
 *    uploadImage metodu frontend'den gelen MultipartFile dosyasını Cloudinary'e yükler.
 *
 * 7. URL'den görsel yükleme
 *    uploadImageFromUrl() metodu özellikle AI tarafından üretilen geçici görsel URL'lerini
 *    kalıcı hale getirmek için kullanılır.
 *
 *    URL doğrudan Cloudinary'e verilmek yerine Java tarafında byte[] olarak indirilir.
 *    Bunun sebebi bazı AI servislerinin verdiği URL'lerin geçici, erişim süresi sınırlı
 *    veya Cloudinary tarafından doğrudan fetch edilemeyebilecek yapıda olmasıdır.
 *
 *    İndirilen byte[] daha sonra normal dosya yükleme gibi Cloudinary'e gönderilir.
 *
 * 8. Görsel silme
 *    deleteImage metodu hem Cloudinary'deki dosyayı hem de DB'deki ProductImage
 *    kaydını kaldırır.
 *
 * 9. Görsel sırası güncelleme
 *    updateImageOrder metodu ürün görsellerinin displayOrder değerini günceller.
 *    Bu alan frontend'de hangi görselin ana görsel olarak veya hangi sırada
 *    gösterileceğini belirlemek için kullanılır.
 *
 */

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductMapper productMapper;
    private final Cloudinary cloudinary;
    private final ProductEventPublisher productEventPublisher;
    private final ProductImageAccess productImageAccess;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Kategori bulunamadı: " + request.getCategoryId()));

        Brand brand = null;
        if (request.getBrandId() != null) {
            brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new NotFoundException("Marka bulunamadı: " + request.getBrandId()));
        }

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(category)
                .brand(brand)
                .build();

        Product saved = productRepository.save(product);
        log.info("Ürün oluşturuldu: id={}, name={}", saved.getId(), saved.getName());

        productEventPublisher.publishCreated(saved);

        return productMapper.toProductResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = findActiveProduct(id);

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Kategori bulunamadı"));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCategory(category);

        if (request.getBrandId() != null) {
            Brand brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new NotFoundException("Marka bulunamadı"));
            product.setBrand(brand);
        }

        product.setEmbeddingGenerated(false);
        Product saved = productRepository.save(product);
        productEventPublisher.publishUpdated(saved);

        return productMapper.toProductResponse(saved);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = findActiveProduct(id);
        product.setActive(false);
        productRepository.save(product);
        productEventPublisher.publishDeleted(id);
        log.info("Ürün pasif yapıldı: id={}", id);
    }

    @Override
    public ProductResponse getProductById(Long id) {
        return productMapper.toProductResponse(findActiveProduct(id));
    }

    private static final int MAX_BATCH_IDS = 80;

    @Override
    public List<ProductBriefResponse> batchActiveSummaries(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Long> capped = ids.stream().distinct().limit(MAX_BATCH_IDS).toList();
        List<Product> rows = productRepository.findByIdInAndActiveTrue(capped);
        Map<Long, Product> byId = rows.stream().collect(java.util.stream.Collectors.toMap(Product::getId, Function.identity()));
        return capped.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .map(ProductBriefResponse::from)
                .toList();
    }

    @Override
    public PageResponse<ProductResponse> listProducts(ProductFilterRequest filter) {
        Specification<Product> spec = Specification.where(ProductSpecification.isActive());

        if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
            spec = spec.and(ProductSpecification.nameContains(filter.getKeyword()));
        }
        if (filter.getCategoryId() != null) {
            spec = spec.and(ProductSpecification.inCategory(filter.getCategoryId()));
        }
        if (filter.getBrandId() != null) {
            spec = spec.and(ProductSpecification.fromBrand(filter.getBrandId()));
        }
        if (filter.getMinPrice() != null) {
            spec = spec.and(ProductSpecification.priceGreaterThanOrEqual(filter.getMinPrice()));
        }
        if (filter.getMaxPrice() != null) {
            spec = spec.and(ProductSpecification.priceLessThanOrEqual(filter.getMaxPrice()));
        }

        Sort sort = Sort.by(
            Sort.Direction.fromString(filter.getSortDir()),
            filter.getSortBy()
        );
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);

        Page<Product> page = productRepository.findAll(spec, pageable);

        return PageResponse.of(
            page.map(productMapper::toProductResponse)
        );
    }

    @Override
    @Transactional
    public ProductResponse uploadImage(Long productId, MultipartFile file, int displayOrder) {
        Product product = findActiveProduct(productId);
        productImageAccess.checkCanManage(product);

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                    "folder", "ecommerce/products",
                    "resource_type", "image"
                )
            );

            ProductImage image = ProductImage.builder()
                    .publicId(uploadResult.get("public_id").toString())
                    .url(uploadResult.get("secure_url").toString())
                    .displayOrder(displayOrder)
                    .build();

            product.addImage(image);
            Product saved = productRepository.save(product);

            log.info("Görsel yüklendi: productId={}, publicId={}", productId, image.getPublicId());
            return productMapper.toProductResponse(saved);

        } catch (Exception e) {
            log.error("Cloudinary yükleme hatası: {}", e.getMessage());
            throw new BusinessException("Görsel yüklenirken hata oluştu");
        }
    }

    @Override
    @Transactional
    public ProductResponse uploadImageFromUrl(Long productId, String imageUrl, int displayOrder) {
        Product product = findActiveProduct(productId);
        productImageAccess.checkCanManage(product);
        try {
            byte[] imageBytes = new java.net.URL(imageUrl).openStream().readAllBytes();
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                imageBytes,
                ObjectUtils.asMap(
                    "folder",        "ecommerce/products",
                    "resource_type", "image"
                )
            );
            ProductImage image = ProductImage.builder()
                    .publicId(uploadResult.get("public_id").toString())
                    .url(uploadResult.get("secure_url").toString())
                    .displayOrder(displayOrder)
                    .build();
            product.addImage(image);
            Product saved = productRepository.save(product);
            log.info("URL'den görsel yüklendi: productId={}, publicId={}", productId, image.getPublicId());
            return productMapper.toProductResponse(saved);
        } catch (Exception e) {
            log.error("URL'den Cloudinary yükleme hatası: {}", e.getMessage());
            throw new BusinessException("Görsel URL'den yüklenirken hata oluştu: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteImage(Long productId, Long imageId) {
        Product product = findActiveProduct(productId);
        productImageAccess.checkCanManage(product);

        ProductImage image = product.getImages().stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Görsel bulunamadı"));

        try {
            cloudinary.uploader().destroy(image.getPublicId(), ObjectUtils.emptyMap());
        } catch (Exception e) {
            log.warn("Cloudinary silme hatası (devam ediliyor): {}", e.getMessage());
        }

        product.removeImage(image);
        productRepository.save(product);
    }

    @Override
    @Transactional
    public void updateImageOrder(Long productId, Long imageId, int displayOrder) {
        Product product = findActiveProduct(productId);
        productImageAccess.checkCanManage(product);
        ProductImage image = product.getImages().stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Görsel bulunamadı"));
        image.setDisplayOrder(displayOrder);
        productRepository.save(product);
        log.debug("Görsel sırası güncellendi: productId={}, imageId={}, order={}", productId, imageId, displayOrder);
    }

    private Product findActiveProduct(Long id) {
        return productRepository.findById(id)
                .filter(Product::isActive)
                .orElseThrow(() -> new NotFoundException("Ürün bulunamadı: " + id));
    }
}
