package com.ecommerce.product.service;

import com.ecommerce.product.dto.request.ProductListingRequest;
import com.ecommerce.product.dto.response.ProductListingResponse;

import java.util.List;

public interface ProductListingService {
    ProductListingResponse createListing(Long productId, Long sellerId, ProductListingRequest request);
    ProductListingResponse updateListingPrice(Long productId, Long sellerId, ProductListingRequest request);
    void deactivateListing(Long productId, Long sellerId);
    void activateListing(Long productId, Long sellerId);
    List<ProductListingResponse> getListingsForProduct(Long productId);
    List<ProductListingResponse> getSellerListings(Long sellerId);
}
