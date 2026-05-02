package com.ecommerce.stock.service;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.NotFoundException;
import com.ecommerce.stock.dto.request.StockAdjustRequest;
import com.ecommerce.stock.dto.request.StockCreateRequest;
import com.ecommerce.stock.dto.response.StockResponse;
import com.ecommerce.stock.entity.Stock;
import com.ecommerce.stock.repository.StockMovementRepository;
import com.ecommerce.stock.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * StockServiceImpl birim testleri — per-seller stok desteği ile güncellendi.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StockService Testleri")
class StockServiceImplTest {

    @Mock private StockRepository stockRepository;
    @Mock private StockMovementRepository movementRepository;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private StockServiceImpl stockService;

    private Stock sampleStock;
    private static final Long PRODUCT_ID = 1L;
    private static final Long SELLER_ID = 10L;

    @BeforeEach
    void setUp() {
        sampleStock = Stock.builder()
                .productId(PRODUCT_ID)
                .sellerId(SELLER_ID)
                .quantity(100)
                .reservedQty(10)
                .lowStockThreshold(5)
                .build();
    }

    // ─── reserveStock() ───────────────────────────────────────────────────────
    @Nested
    @DisplayName("reserveStock() — Sipariş stok rezervasyonu")
    class ReserveStockTests {

        @Test
        @DisplayName("Per-seller: yeterli stok → rezervasyon başarılı")
        void reserveStock_perSeller_success() {
            given(stockRepository.reserveStockForSeller(PRODUCT_ID, SELLER_ID, 5)).willReturn(1);
            given(stockRepository.findByProductIdAndSellerId(PRODUCT_ID, SELLER_ID))
                    .willReturn(Optional.of(sampleStock));

            boolean result = stockService.reserveStock(PRODUCT_ID, SELLER_ID, 5, "ORD-001");

            assertThat(result).isTrue();
            then(movementRepository).should().save(any());
            then(rabbitTemplate).should().convertAndSend(anyString(), contains("reserved"), any(Object.class));
        }

        @Test
        @DisplayName("Per-seller: yetersiz stok → false, FAILED eventi (legacy fallback yok)")
        void reserveStock_perSeller_insufficientStock() {
            given(stockRepository.reserveStockForSeller(PRODUCT_ID, SELLER_ID, 200)).willReturn(0);
            given(stockRepository.existsByProductIdAndSellerId(PRODUCT_ID, SELLER_ID)).willReturn(true);

            boolean result = stockService.reserveStock(PRODUCT_ID, SELLER_ID, 200, "ORD-001");

            assertThat(result).isFalse();
            then(movementRepository).should(never()).save(any());
            then(stockRepository).should(never()).reserveStockLegacy(anyLong(), anyInt());
            then(rabbitTemplate).should().convertAndSend(anyString(), contains("failed"), any(Object.class));
        }

        @Test
        @DisplayName("Seller stok satırı yoksa legacy stoğa düşer ve başarılı olur")
        void reserveStock_fallsBackToLegacy_whenNoSellerStockRow() {
            Stock legacyStock = Stock.builder()
                    .productId(PRODUCT_ID)
                    .quantity(50)
                    .reservedQty(0)
                    .lowStockThreshold(5)
                    .build();
            given(stockRepository.reserveStockForSeller(PRODUCT_ID, SELLER_ID, 5)).willReturn(0);
            given(stockRepository.existsByProductIdAndSellerId(PRODUCT_ID, SELLER_ID)).willReturn(false);
            given(stockRepository.reserveStockLegacy(PRODUCT_ID, 5)).willReturn(1);
            given(stockRepository.findByProductIdAndSellerIdIsNull(PRODUCT_ID))
                    .willReturn(Optional.of(legacyStock));

            boolean result = stockService.reserveStock(PRODUCT_ID, SELLER_ID, 5, "ORD-fallback");

            assertThat(result).isTrue();
            then(movementRepository).should().save(any());
            then(stockRepository).should().reserveStockLegacy(PRODUCT_ID, 5);
        }

        @Test
        @DisplayName("Legacy (sellerId=null): yeterli stok → rezervasyon başarılı")
        void reserveStock_legacy_success() {
            Stock legacyStock = Stock.builder()
                    .productId(PRODUCT_ID)
                    .quantity(50)
                    .reservedQty(0)
                    .lowStockThreshold(5)
                    .build();
            given(stockRepository.reserveStockLegacy(PRODUCT_ID, 5)).willReturn(1);
            given(stockRepository.findByProductIdAndSellerIdIsNull(PRODUCT_ID))
                    .willReturn(Optional.of(legacyStock));

            boolean result = stockService.reserveStock(PRODUCT_ID, null, 5, "ORD-002");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Stok düşük eşiğin altına düşünce LOW_STOCK eventi yayınlanır")
        void reserveStock_triggersLowStockAlert() {
            Stock lowStock = Stock.builder()
                    .productId(2L)
                    .sellerId(SELLER_ID)
                    .quantity(7)
                    .reservedQty(5)
                    .lowStockThreshold(5)
                    .build();
            given(stockRepository.reserveStockForSeller(2L, SELLER_ID, 1)).willReturn(1);
            given(stockRepository.findByProductIdAndSellerId(2L, SELLER_ID))
                    .willReturn(Optional.of(lowStock));

            stockService.reserveStock(2L, SELLER_ID, 1, "ORD-003");

            then(rabbitTemplate).should(atLeastOnce()).convertAndSend(anyString(), anyString(), any(Object.class));
        }
    }

    // ─── releaseStock() ───────────────────────────────────────────────────────
    @Nested
    @DisplayName("releaseStock() — Rezervasyon iadesi")
    class ReleaseStockTests {

        @Test
        @DisplayName("Başarılı iade → true, hareket kaydedilir")
        void releaseStock_success() {
            given(stockRepository.releaseReservationForSeller(PRODUCT_ID, SELLER_ID, 5)).willReturn(1);
            given(stockRepository.findByProductIdAndSellerId(PRODUCT_ID, SELLER_ID))
                    .willReturn(Optional.of(sampleStock));

            boolean result = stockService.releaseStock(PRODUCT_ID, SELLER_ID, 5, "ORD-001");

            assertThat(result).isTrue();
            then(movementRepository).should().save(any());
        }

        @Test
        @DisplayName("İade edilecek rezervasyon yok → false")
        void releaseStock_nothingToRelease() {
            given(stockRepository.releaseReservationForSeller(PRODUCT_ID, SELLER_ID, 999)).willReturn(0);
            given(stockRepository.existsByProductIdAndSellerId(PRODUCT_ID, SELLER_ID)).willReturn(true);

            boolean result = stockService.releaseStock(PRODUCT_ID, SELLER_ID, 999, "ORD-001");

            assertThat(result).isFalse();
            then(movementRepository).should(never()).save(any());
            then(stockRepository).should(never()).releaseReservationLegacy(anyLong(), anyInt());
        }
    }

    // ─── adjustStock() ────────────────────────────────────────────────────────
    @Nested
    @DisplayName("adjustStock() — Manuel stok düzeltme")
    class AdjustStockTests {

        @Test
        @DisplayName("Geçerli düzeltme → stok güncellenir")
        void adjustStock_valid() {
            given(stockRepository.findByProductIdAndSellerId(PRODUCT_ID, SELLER_ID))
                    .willReturn(Optional.of(sampleStock));
            given(stockRepository.save(sampleStock)).willReturn(sampleStock);

            StockAdjustRequest request = new StockAdjustRequest();
            request.setQuantity(50);
            request.setReason("Yeniden sayım");

            StockResponse response = stockService.adjustStock(PRODUCT_ID, SELLER_ID, request);

            assertThat(sampleStock.getQuantity()).isEqualTo(50);
            then(movementRepository).should().save(any());
        }

        @Test
        @DisplayName("Yeni miktar < reservedQty → BusinessException")
        void adjustStock_belowReserved_throws() {
            given(stockRepository.findByProductIdAndSellerId(PRODUCT_ID, SELLER_ID))
                    .willReturn(Optional.of(sampleStock));

            StockAdjustRequest request = new StockAdjustRequest();
            request.setQuantity(5); // reservedQty=10, bu geçersiz
            request.setReason("Test");

            assertThatThrownBy(() -> stockService.adjustStock(PRODUCT_ID, SELLER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("rezerve miktarından");

            then(stockRepository).should(never()).save(any());
        }
    }

    // ─── createOrUpdateStock() ────────────────────────────────────────────────
    @Nested
    @DisplayName("createOrUpdateStock()")
    class CreateStockTests {

        @Test
        @DisplayName("Yeni seller stoku oluşturulur")
        void createStock_newSellerStock_success() {
            Long productId = 99L;
            given(stockRepository.findByProductIdAndSellerId(productId, SELLER_ID))
                    .willReturn(Optional.empty());
            given(stockRepository.save(any(Stock.class))).willReturn(
                    Stock.builder().productId(productId).sellerId(SELLER_ID).quantity(50).lowStockThreshold(5).build());

            StockCreateRequest request = new StockCreateRequest();
            request.setProductId(productId);
            request.setSellerId(SELLER_ID);
            request.setInitialQuantity(50);
            request.setLowStockThreshold(5);

            StockResponse response = stockService.createOrUpdateStock(request);

            assertThat(response.getProductId()).isEqualTo(productId);
            assertThat(response.getQuantity()).isEqualTo(50);
            then(movementRepository).should().save(any());
        }

        @Test
        @DisplayName("Mevcut stok güncellenir (upsert)")
        void createStock_existing_updates() {
            given(stockRepository.findByProductIdAndSellerId(PRODUCT_ID, SELLER_ID))
                    .willReturn(Optional.of(sampleStock));
            given(stockRepository.save(sampleStock)).willReturn(sampleStock);

            StockCreateRequest request = new StockCreateRequest();
            request.setProductId(PRODUCT_ID);
            request.setSellerId(SELLER_ID);
            request.setInitialQuantity(200);
            request.setLowStockThreshold(10);

            stockService.createOrUpdateStock(request);

            assertThat(sampleStock.getQuantity()).isEqualTo(200);
        }
    }
}
