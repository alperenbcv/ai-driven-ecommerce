package com.ecommerce.product.service;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.NotFoundException;
import com.ecommerce.product.dto.request.CategoryRequest;
import com.ecommerce.product.dto.response.CategoryResponse;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.mapper.ProductMapper;
import com.ecommerce.product.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    @Override
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll().stream()
                .filter(Category::isActive)
                .map(productMapper::toCategoryResponse)
                .toList();
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        categoryRepository.findByNameIgnoreCase(request.getName()).ifPresent(c -> {
            throw new BusinessException("Bu isimde kategori zaten mevcut: " + request.getName());
        });

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .build();

        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new NotFoundException("Üst kategori bulunamadı"));
            category.setParent(parent);
        }

        return productMapper.toCategoryResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = findActiveCategory(id);
        productMapper.updateCategoryFromRequest(request, category);

        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new BusinessException("Kategori kendi kendinin alt kategorisi olamaz");
            }
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new NotFoundException("Üst kategori bulunamadı"));
            category.setParent(parent);
        }

        return productMapper.toCategoryResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = findActiveCategory(id);
        category.setActive(false);
        categoryRepository.save(category);
    }

    @Override
    public List<CategoryResponse> getRootCategories() {
        return productMapper.toCategoryResponseList(categoryRepository.findRootCategories());
    }

    @Override
    public List<CategoryResponse> getSubCategories(Long parentId) {
        return productMapper.toCategoryResponseList(
            categoryRepository.findByParentIdAndActiveTrue(parentId)
        );
    }

    private Category findActiveCategory(Long id) {
        return categoryRepository.findById(id)
                .filter(Category::isActive)
                .orElseThrow(() -> new NotFoundException("Kategori bulunamadı: " + id));
    }
}
