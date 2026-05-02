package com.ecommerce.product.service;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.NotFoundException;
import com.ecommerce.product.dto.request.ReviewRequest;
import com.ecommerce.product.dto.response.ReviewResponse;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.ProductReview;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.repository.ProductReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * ReviewServiceImpl için birim testleri.
 *
 * Bu service'in en kritik iş kuralları:
 * 1. Aynı kullanıcı bir ürüne iki kez yorum yapamaz
 * 2. Yorum eklenince Product'ın averageRating güncellenir (addReviewRating)
 * 3. Yorum silinince Product'ın averageRating düzeltilir (removeReviewRating)
 * 4. Admin başkasının yorumunu silebilir, kullanıcı sadece kendi yorumunu
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService Testleri")
class ReviewServiceImplTest {

    @Mock private ProductReviewRepository reviewRepository;
    @Mock private ProductRepository productRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private Product sampleProduct;
    private ProductReview sampleReview;

    @BeforeEach
    void setUp() {
        sampleProduct = Product.builder()
                .name("Gaming Monitör")
                .description("144Hz IPS panel")
                .price(new BigDecimal("4999.99"))
                .averageRating(new BigDecimal("4.0"))
                .reviewCount(5)
                .build();
        sampleProduct.setActive(true);
        sampleProduct.setId(1L);

        sampleReview = ProductReview.builder()
                .product(sampleProduct)
                .userId(42L)
                .userName("Ali Yılmaz")
                .rating(5)
                .title("Harika ürün!")
                .body("Çok memnun kaldım, kesinlikle tavsiye ederim.")
                .build();
        sampleReview.setId(100L);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  addReview() testleri
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("addReview()")
    class AddReviewTests {

        @Test
        @DisplayName("İlk kez yorum → başarılı, ürün rating güncellenir")
        void addReview_firstTime_success() {
            // GIVEN
            ReviewRequest request = new ReviewRequest();
            request.setRating(5);
            request.setTitle("Mükemmel!");
            request.setBody("Çok memnun kaldım.");
            given(productRepository.findById(1L)).willReturn(Optional.of(sampleProduct));
            given(reviewRepository.existsByProductIdAndUserId(1L, 42L)).willReturn(false);
            given(reviewRepository.save(any(ProductReview.class))).willReturn(sampleReview);
            given(productRepository.save(any(Product.class))).willReturn(sampleProduct);

            // WHEN
            ReviewResponse response = reviewService.addReview(1L, 42L, "Ali Yılmaz", request);

            // THEN
            assertThat(response).isNotNull();
            assertThat(response.getRating()).isEqualTo(5);
            // Service, save'in dönüşünü değil lokal review nesnesini döndürür
            assertThat(response.getTitle()).isEqualTo("Mükemmel!");

            // Product.averageRating güncellendi mi? (save çağrıldı mı?)
            then(productRepository).should().save(sampleProduct);

            // Yorum kaydedildi mi?
            then(reviewRepository).should().save(any(ProductReview.class));
        }

        @Test
        @DisplayName("Aynı kullanıcı ikinci kez yorum → BusinessException")
        void addReview_duplicateReview_throwsBusinessException() {
            // GIVEN
            ReviewRequest request = new ReviewRequest();
            request.setRating(3);
            request.setTitle("Orta");
            request.setBody("İdare eder.");
            given(productRepository.findById(1L)).willReturn(Optional.of(sampleProduct));
            given(reviewRepository.existsByProductIdAndUserId(1L, 42L)).willReturn(true); // Zaten var!

            // WHEN & THEN
            assertThatThrownBy(() -> reviewService.addReview(1L, 42L, "Ali", request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("zaten yorum yaptınız");

            // Yorum kaydedilmemeli, ürün de değişmemeli
            then(reviewRepository).should(never()).save(any());
            then(productRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("Pasif ürüne yorum → NotFoundException")
        void addReview_inactiveProduct_throwsNotFoundException() {
            // GIVEN
            sampleProduct.setActive(false);
            given(productRepository.findById(1L)).willReturn(Optional.of(sampleProduct));

            // WHEN & THEN
            ReviewRequest rq = new ReviewRequest(); rq.setRating(4); rq.setTitle("T"); rq.setBody("B");
            assertThatThrownBy(() -> reviewService.addReview(1L, 42L, "Ali", rq))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  deleteReview() testleri
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("deleteReview()")
    class DeleteReviewTests {

        @Test
        @DisplayName("Kullanıcı kendi yorumunu siler → başarılı, rating güncellenir")
        void deleteReview_ownReview_success() {
            // GIVEN
            given(reviewRepository.findById(100L)).willReturn(Optional.of(sampleReview));
            given(productRepository.save(sampleProduct)).willReturn(sampleProduct);

            // WHEN
            reviewService.deleteReview(1L, 100L, 42L, false);

            // THEN
            then(reviewRepository).should().delete(sampleReview);
            then(productRepository).should().save(sampleProduct); // Rating güncellendi mi?
        }

        @Test
        @DisplayName("Admin başkasının yorumunu siler → başarılı (isAdmin=true)")
        void deleteReview_adminDeletesOthersReview_success() {
            // GIVEN
            given(reviewRepository.findById(100L)).willReturn(Optional.of(sampleReview));
            given(productRepository.save(sampleProduct)).willReturn(sampleProduct);

            // WHEN: adminId=999 (farklı kişi), ama isAdmin=true
            reviewService.deleteReview(1L, 100L, 999L, true);

            // THEN: silme gerçekleşmeli
            then(reviewRepository).should().delete(sampleReview);
        }

        @Test
        @DisplayName("Kullanıcı başkasının yorumunu silmeye çalışır → BusinessException")
        void deleteReview_otherUsersReview_throwsBusinessException() {
            // GIVEN
            given(reviewRepository.findById(100L)).willReturn(Optional.of(sampleReview)); // userId=42

            // WHEN & THEN: userId=99 başkasının yorumunu silmeye çalışıyor, isAdmin=false
            assertThatThrownBy(() -> reviewService.deleteReview(1L, 100L, 99L, false))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("yetkiniz yok");

            // Silme gerçekleşmemeli
            then(reviewRepository).should(never()).delete(any());
        }

        @Test
        @DisplayName("Yorum farklı ürüne ait → BusinessException")
        void deleteReview_wrongProduct_throwsBusinessException() {
            // GIVEN
            given(reviewRepository.findById(100L)).willReturn(Optional.of(sampleReview)); // productId=1

            // WHEN & THEN: productId=99 ile silmeye çalışıyoruz
            assertThatThrownBy(() -> reviewService.deleteReview(99L, 100L, 42L, false))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("bu ürüne ait değil");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  markHelpful() testi
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("markHelpful()")
    class MarkHelpfulTests {

        @Test
        @DisplayName("Faydalı işaretle → helpfulCount artar")
        void markHelpful_incrementsCount() {
            // GIVEN
            sampleReview.setHelpfulCount(5);
            given(reviewRepository.findById(100L)).willReturn(Optional.of(sampleReview));
            given(reviewRepository.save(sampleReview)).willReturn(sampleReview);

            // WHEN
            reviewService.markHelpful(100L);

            // THEN
            assertThat(sampleReview.getHelpfulCount()).isEqualTo(6);
            then(reviewRepository).should().save(sampleReview);
        }
    }
}
