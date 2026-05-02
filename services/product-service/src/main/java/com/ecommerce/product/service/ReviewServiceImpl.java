package com.ecommerce.product.service;

import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.NotFoundException;
import com.ecommerce.product.dto.request.ReviewRequest;
import com.ecommerce.product.dto.response.ReviewResponse;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.ProductReview;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.repository.ProductReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {

    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public ReviewResponse addReview(Long productId, Long userId, String userName, ReviewRequest request) {
        Product product = findActiveProduct(productId);

        if (reviewRepository.existsByProductIdAndUserId(productId, userId)) {
            throw new BusinessException("Bu ürüne zaten yorum yaptınız");
        }

        ProductReview review = ProductReview.builder()
                .product(product)
                .userId(userId)
                .userName(userName)
                .rating(request.getRating())
                .title(request.getTitle())
                .body(request.getBody())
                .build();

        reviewRepository.save(review);

        product.addReviewRating(request.getRating());
        productRepository.save(product);

        log.info("Yorum eklendi: productId={}, userId={}, rating={}", productId, userId, request.getRating());
        return toResponse(review);
    }

    @Override
    @Transactional
    public void deleteReview(Long productId, Long reviewId, Long userId, boolean isAdmin) {
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Yorum bulunamadı"));

        if (!review.getProduct().getId().equals(productId)) {
            throw new BusinessException("Yorum bu ürüne ait değil");
        }

        if (!isAdmin && !review.getUserId().equals(userId)) {
            throw new BusinessException("Bu yorumu silme yetkiniz yok");
        }

        Product product = review.getProduct();
        product.removeReviewRating(review.getRating());
        productRepository.save(product);

        reviewRepository.delete(review);
        log.info("Yorum silindi: reviewId={}, productId={}", reviewId, productId);
    }

    @Override
    @Transactional
    public void markHelpful(Long reviewId) {
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Yorum bulunamadı"));
        review.setHelpfulCount(review.getHelpfulCount() + 1);
        reviewRepository.save(review);
    }

    @Override
    public PageResponse<ReviewResponse> getReviews(Long productId, int page, int size) {
        Page<ProductReview> reviews = reviewRepository
                .findByProductIdAndApprovedTrueOrderByCreatedAtDesc(
                        productId, PageRequest.of(page, size));
        return PageResponse.of(reviews.map(this::toResponse));
    }

    private ReviewResponse toResponse(ProductReview review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .userId(review.getUserId())
                .userName(review.getUserName())
                .rating(review.getRating())
                .title(review.getTitle())
                .body(review.getBody())
                .verifiedPurchase(review.isVerifiedPurchase())
                .helpfulCount(review.getHelpfulCount())
                .createdAt(review.getCreatedAt())
                .build();
    }

    private Product findActiveProduct(Long id) {
        return productRepository.findById(id)
                .filter(Product::isActive)
                .orElseThrow(() -> new NotFoundException("Ürün bulunamadı: " + id));
    }
}
