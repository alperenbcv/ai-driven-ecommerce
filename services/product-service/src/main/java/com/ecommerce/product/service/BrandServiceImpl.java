package com.ecommerce.product.service;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.NotFoundException;
import com.ecommerce.product.dto.request.BrandRequest;
import com.ecommerce.product.dto.response.BrandResponse;
import com.ecommerce.product.entity.Brand;
import com.ecommerce.product.mapper.ProductMapper;
import com.ecommerce.product.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public BrandResponse createBrand(BrandRequest request) {
        brandRepository.findByNameIgnoreCase(request.getName()).ifPresent(b -> {
            throw new BusinessException("Bu isimde marka zaten mevcut: " + request.getName());
        });

        Brand brand = Brand.builder()
                .name(request.getName())
                .logoUrl(request.getLogoUrl())
                .build();

        return productMapper.toBrandResponse(brandRepository.save(brand));
    }

    @Override
    @Transactional
    public BrandResponse updateBrand(Long id, BrandRequest request) {
        Brand brand = findActiveBrand(id);
        productMapper.updateBrandFromRequest(request, brand);
        return productMapper.toBrandResponse(brandRepository.save(brand));
    }

    @Override
    @Transactional
    public void deleteBrand(Long id) {
        Brand brand = findActiveBrand(id);
        brand.setActive(false);
        brandRepository.save(brand);
    }

    @Override
    public List<BrandResponse> getAllBrands() {
        return productMapper.toBrandResponseList(brandRepository.findAll());
    }

    private Brand findActiveBrand(Long id) {
        return brandRepository.findById(id)
                .filter(Brand::isActive)
                .orElseThrow(() -> new NotFoundException("Marka bulunamadı: " + id));
    }
}
