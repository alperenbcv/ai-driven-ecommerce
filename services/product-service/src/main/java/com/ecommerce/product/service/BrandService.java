package com.ecommerce.product.service;

import com.ecommerce.product.dto.request.BrandRequest;
import com.ecommerce.product.dto.response.BrandResponse;

import java.util.List;

public interface BrandService {
    BrandResponse createBrand(BrandRequest request);
    BrandResponse updateBrand(Long id, BrandRequest request);
    void deleteBrand(Long id);
    List<BrandResponse> getAllBrands();
}
