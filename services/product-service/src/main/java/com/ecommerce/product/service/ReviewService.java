package com.ecommerce.product.service;

import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.product.dto.request.ReviewRequest;
import com.ecommerce.product.dto.response.ReviewResponse;

public interface ReviewService {
    ReviewResponse addReview(Long productId, Long userId, String userName, ReviewRequest request);
    void deleteReview(Long productId, Long reviewId, Long userId, boolean isAdmin);
    void markHelpful(Long reviewId);
    PageResponse<ReviewResponse> getReviews(Long productId, int page, int size);
}
