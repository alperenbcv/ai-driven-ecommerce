package com.ecommerce.product.service;

import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.product.dto.request.ProductFilterRequest;
import com.ecommerce.product.dto.request.ProductRequest;
import com.ecommerce.product.dto.response.ProductBriefResponse;
import com.ecommerce.product.dto.response.ProductResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct(ProductRequest request);
    ProductResponse updateProduct(Long id, ProductRequest request);
    void deleteProduct(Long id);
    ProductResponse getProductById(Long id);
    PageResponse<ProductResponse> listProducts(ProductFilterRequest filter);

    /** Aktif ürünlerin özet kartları; istek sırasına göre (en fazla 80 id). */
    List<ProductBriefResponse> batchActiveSummaries(List<Long> ids);
    ProductResponse uploadImage(Long productId, MultipartFile file, int displayOrder);
    ProductResponse uploadImageFromUrl(Long productId, String imageUrl, int displayOrder);
    void deleteImage(Long productId, Long imageId);
    void updateImageOrder(Long productId, Long imageId, int displayOrder);
}
