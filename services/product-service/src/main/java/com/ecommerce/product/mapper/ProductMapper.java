package com.ecommerce.product.mapper;

import com.ecommerce.product.dto.request.CategoryRequest;
import com.ecommerce.product.dto.request.BrandRequest;
import com.ecommerce.product.dto.response.*;
import com.ecommerce.product.entity.*;
import org.mapstruct.*;

import java.util.List;

/**
 *
 * Bu sınıf Product, Category, Brand ve ProductImage entity'lerini
 * frontend'e döneceğimiz DTO response nesnelerine dönüştürmek için kullanılır.
 *
 * MapStruct bu dönüşümlerin implementation'ını compile time'da otomatik üretir.
 * Böylece her dönüşüm için manuel setter/getter yazmamıza gerek kalmaz.
 *
 * componentModel = "spring":
 * MapStruct tarafından oluşturulan mapper implementation'ı Spring Bean olarak
 * kaydedilir. Bu sayede service katmanında constructor injection ile kullanılabilir.
 *
 * unmappedTargetPolicy = ReportingPolicy.IGNORE:
 * DTO'da olup entity'de olmayan veya bilinçli olarak map edilmeyen alanlar için
 * compile-time hata verilmesini engeller.
 *
 * Nested mapping:
 * ProductResponse içinde category ve brand alanları summary DTO olarak tutulur.
 * Bu yüzden product.category.id, product.category.name gibi nested alanlar
 * açıkça belirtilmiştir.
 *
 * updateCategoryFromRequest / updateBrandFromRequest:
 * Mevcut entity'yi tamamen yeniden oluşturmak yerine, gelen request verisiyle
 * var olan entity güncellenir.
 *
 * @MappingTarget:
 * Yeni nesne üretmek yerine parametre olarak verilen mevcut entity üzerinde
 * güncelleme yapılacağını belirtir.
 *
 * NullValuePropertyMappingStrategy.IGNORE:
 * Request içinde null gelen alanlar entity'deki mevcut değeri silmez.
 * Böylece partial update benzeri güvenli bir güncelleme davranışı elde edilir.
 *
 * id, parent ve children alanlarının ignore edilmesi:
 * - id DB tarafından yönetilir, request ile değiştirilmemelidir.
 * - parent ilişkisi service katmanında kontrollü şekilde set edilir.
 * - children koleksiyonu request üzerinden doğrudan güncellenmez.
 */

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {

    @Mapping(target = "category.id", source = "category.id")
    @Mapping(target = "category.name", source = "category.name")
    @Mapping(target = "brand.id", source = "brand.id")
    @Mapping(target = "brand.name", source = "brand.name")
    ProductResponse toProductResponse(Product product);

    List<ProductResponse> toProductResponseList(List<Product> products);

    ProductImageResponse toProductImageResponse(ProductImage image);

    @Mapping(target = "parentId", source = "parent.id")
    CategoryResponse toCategoryResponse(Category category);

    List<CategoryResponse> toCategoryResponseList(List<Category> categories);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", ignore = true)
    void updateCategoryFromRequest(CategoryRequest request, @MappingTarget Category category);

    BrandResponse toBrandResponse(Brand brand);

    List<BrandResponse> toBrandResponseList(List<Brand> brands);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    void updateBrandFromRequest(BrandRequest request, @MappingTarget Brand brand);
}
