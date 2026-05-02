package com.ecommerce.product.service;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.NotFoundException;
import com.ecommerce.product.client.UserServiceClient;
import com.ecommerce.product.dto.request.ProductListingRequest;
import com.ecommerce.product.dto.response.ProductListingResponse;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.ProductListing;
import com.ecommerce.product.repository.ProductListingRepository;
import com.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductListingServiceImpl implements ProductListingService {

    private final ProductListingRepository listingRepository;
    private final ProductRepository productRepository;
    private final UserServiceClient userServiceClient;

    /**
     * Seller, katalogdan bir ürünü seçip listing oluşturur.
     *
     * Akış:
     * 1. productId ile katalogu doğrula (ürün aktif mi?)
     * 2. Bu seller bu ürüne zaten listing açmış mı? → hata ver
     * 3. Listing oluştur: sadece productId + sellerId + price
     *
     * Seller burada ürün adı yazmıyor ürünün template'i zaten var.
     * Stock Service için ayrıca stok kaydı açması gerekecek.
     */
    @Override
    @Transactional
    public ProductListingResponse createListing(Long productId, Long sellerId, ProductListingRequest request) {
        Product product = productRepository.findById(productId)
                .filter(Product::isActive)
                .orElseThrow(() -> new NotFoundException("Ürün bulunamadı: " + productId));

        if (listingRepository.existsByProductIdAndSellerId(productId, sellerId)) {
            throw new BusinessException("Bu ürün için zaten aktif bir listeniz var. Fiyatı güncellemek için PATCH kullanın.");
        }

        ProductListing listing = ProductListing.builder()
                .product(product)
                .sellerId(sellerId)
                .price(request.getPrice())
                .build();

        ProductListing saved = listingRepository.save(listing);
        log.info("Listing oluşturuldu: productId={}, sellerId={}, price={}", productId, sellerId, request.getPrice());
        return toResponse(saved);
    }

    /** Seller sadece kendi listing'inin fiyatını değiştirebilir */
    @Override
    @Transactional
    public ProductListingResponse updateListingPrice(Long productId, Long sellerId, ProductListingRequest request) {
        ProductListing listing = listingRepository.findByProductIdAndSellerId(productId, sellerId)
                .orElseThrow(() -> new NotFoundException("Bu ürün için listeniz bulunamadı"));

        listing.setPrice(request.getPrice());
        listing.setActive(true);
        log.info("Listing fiyatı güncellendi: productId={}, sellerId={}, yeniFixed={}", productId, sellerId, request.getPrice());
        return toResponse(listingRepository.save(listing));
    }

    /** Seller listing'ini geçici olarak devre dışı bırakır (stok yok vb.) */
    @Override
    @Transactional
    public void deactivateListing(Long productId, Long sellerId) {
        ProductListing listing = listingRepository.findByProductIdAndSellerId(productId, sellerId)
                .orElseThrow(() -> new NotFoundException("Listing bulunamadı"));
        listing.setActive(false);
        listingRepository.save(listing);
        log.info("Listing pasif yapıldı: productId={}, sellerId={}", productId, sellerId);
    }

    /** Seller pasif listing'ini yeniden aktif eder */
    @Override
    @Transactional
    public void activateListing(Long productId, Long sellerId) {
        ProductListing listing = listingRepository.findByProductIdAndSellerId(productId, sellerId)
                .orElseThrow(() -> new NotFoundException("Listing bulunamadı"));
        listing.setActive(true);
        listingRepository.save(listing);
        log.info("Listing aktif edildi: productId={}, sellerId={}", productId, sellerId);
    }

    /** Bir ürünün tüm aktif listing'lerini fiyata göre sıralı getir */
    @Override
    public List<ProductListingResponse> getListingsForProduct(Long productId) {
        return listingRepository
                .findByProductIdAndActiveTrueOrderByPriceAsc(productId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** Seller kendi listing'lerini görür */
    @Override
    public List<ProductListingResponse> getSellerListings(Long sellerId) {
        return listingRepository
                .findBySellerIdOrderByCreatedAtDesc(sellerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ProductListingResponse toResponse(ProductListing l) {
        return ProductListingResponse.builder()
                .id(l.getId())
                .productId(l.getProduct().getId())
                .productName(l.getProduct().getName())
                .sellerId(l.getSellerId())
                .sellerName(userServiceClient.getSellerDisplayName(l.getSellerId()))
                .price(l.getPrice())
                .active(l.isActive())
                .createdAt(l.getCreatedAt())
                .build();
    }
}
