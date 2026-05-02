package com.ecommerce.order.service;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.NotFoundException;
import com.ecommerce.order.client.ProductCatalogClient;
import com.ecommerce.order.dto.request.CreateOrderRequest;
import com.ecommerce.order.dto.response.OrderResponse;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * OrderServiceImpl için birim testleri.
 *
 * EKSTRA KAVRAM — ArgumentCaptor:
 * Bir mock'a geçilen argümanın tam değerini yakalamak için kullanılır.
 * Örneğin: orderRepository.save() çağrısına geçilen Order nesnesini yakalayıp
 * içindeki alanları doğrulayabiliriz.
 *
 * Örnek:
 *   ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
 *   then(orderRepository).should().save(captor.capture());
 *   assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.PENDING);
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Testleri")
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private ProductCatalogClient productCatalogClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    // Test verisi
    private Order sampleOrder;
    private CreateOrderRequest createRequest;

    @BeforeEach
    void setUp() {
        // Hazır bir sipariş nesnesi (PENDING durumunda)
        sampleOrder = Order.builder()
                .orderNumber("ORD-20260501-00001")
                .userId(42L)
                .userEmail("test@test.com")
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("999.99"))
                .shippingFullName("Ali Yılmaz")
                .shippingPhone("05321234567")
                .shippingCity("İstanbul")
                .shippingDistrict("Kadıköy")
                .shippingFullAddress("Test Mahallesi No:1")
                .build();

        // Sipariş ürünü ekle
        OrderItem item = OrderItem.builder()
                .productId(10L)
                .listingId(5L)
                .sellerId(3L)
                .productName("Gaming Monitör 144Hz")
                .unitPrice(new BigDecimal("999.99"))
                .quantity(1)
                .totalPrice(new BigDecimal("999.99"))
                .build();
        sampleOrder.addItem(item);

        // Sipariş oluşturma isteği
        CreateOrderRequest.ShippingAddressRequest addr = new CreateOrderRequest.ShippingAddressRequest();
        addr.setFullName("Ali Yılmaz");
        addr.setPhone("05321234567");
        addr.setCity("İstanbul");
        addr.setDistrict("Kadıköy");
        addr.setFullAddress("Test Mahallesi No:1");

        CreateOrderRequest.OrderItemRequest reqItem = new CreateOrderRequest.OrderItemRequest();
        reqItem.setProductId(10L);
        reqItem.setListingId(5L);
        reqItem.setSellerId(3L);
        reqItem.setProductName("Gaming Monitör 144Hz");
        reqItem.setUnitPrice(new BigDecimal("999.99"));
        reqItem.setQuantity(1);

        createRequest = new CreateOrderRequest();
        createRequest.setUserEmail("test@test.com");
        createRequest.setItems(List.of(reqItem));
        createRequest.setShippingAddress(addr);

        lenient().when(productCatalogClient.getListing(10L, 5L)).thenReturn(
                ProductCatalogClient.ProductListingSnapshot.builder()
                        .id(5L)
                        .productId(10L)
                        .productName("Gaming Monitör 144Hz")
                        .sellerId(3L)
                        .price(new BigDecimal("999.99"))
                        .active(true)
                        .build()
        );
    }

    // ═══════════════════════════════════════════════════════════════════
    //  createOrder() testleri
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("createOrder()")
    class CreateOrderTests {

        @Test
        @DisplayName("Sipariş oluşturulur → PENDING durumda DB'ye kaydedilir, RabbitMQ eventi yayınlanır")
        void createOrder_success() {
            // GIVEN
            given(orderRepository.save(any(Order.class))).willReturn(sampleOrder);

            // WHEN
            OrderResponse response = orderService.createOrder(42L, createRequest);

            // THEN
            assertThat(response).isNotNull();
            assertThat(response.getOrderNumber()).startsWith("ORD-");
            assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(response.getTotalAmount()).isEqualByComparingTo("999.99");

            // DB'ye save edildi mi?
            then(orderRepository).should().save(any(Order.class));

            // RabbitMQ'ya en az 1 event gönderildi mi?
            // (stock.reserve + order.created = 2 convertAndSend çağrısı)
            then(rabbitTemplate).should(atLeastOnce()).convertAndSend(anyString(), anyString(), any(Object.class));
        }

        @Test
        @DisplayName("Toplam tutar doğru hesaplanır (birim fiyat × adet)")
        void createOrder_totalAmountCalculatedCorrectly() {
            // GIVEN — 3 adet × 100 TL = 300 TL
            CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
            item.setProductId(1L);
            item.setListingId(1L);
            item.setSellerId(1L);
            item.setProductName("Test Ürün");
            item.setUnitPrice(new BigDecimal("100.00"));
            item.setQuantity(3);

            CreateOrderRequest req = new CreateOrderRequest();
            req.setUserEmail("test@test.com");
            req.setItems(List.of(item));
            req.setShippingAddress(createRequest.getShippingAddress());

            given(productCatalogClient.getListing(1L, 1L)).willReturn(
                    ProductCatalogClient.ProductListingSnapshot.builder()
                            .id(1L)
                            .productId(1L)
                            .productName("Test Ürün")
                            .sellerId(1L)
                            .price(new BigDecimal("100.00"))
                            .active(true)
                            .build()
            );

            // ArgumentCaptor ile save()'e geçilen Order'ı yakala
            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            Order expectedSaved = sampleOrder;
            expectedSaved.setTotalAmount(new BigDecimal("300.00"));
            given(orderRepository.save(orderCaptor.capture())).willReturn(expectedSaved);

            // WHEN
            orderService.createOrder(42L, req);

            // THEN — save()'e gönderilen Order'ın totalAmount'u doğrula
            Order capturedOrder = orderCaptor.getValue();
            assertThat(capturedOrder.getTotalAmount()).isEqualByComparingTo("300.00");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  getOrder() testleri
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("getOrder()")
    class GetOrderTests {

        @Test
        @DisplayName("Kendi siparişini görmek → başarılı")
        void getOrder_ownOrder_success() {
            // GIVEN
            given(orderRepository.findByOrderNumber("ORD-20260501-00001"))
                    .willReturn(Optional.of(sampleOrder));

            // WHEN
            OrderResponse response = orderService.getOrder("ORD-20260501-00001", 42L);

            // THEN
            assertThat(response.getOrderNumber()).isEqualTo("ORD-20260501-00001");
            assertThat(response.getUserId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("Başkasının siparişine erişim → BusinessException (authorization)")
        void getOrder_otherUsersOrder_throwsBusinessException() {
            // GIVEN
            given(orderRepository.findByOrderNumber("ORD-20260501-00001"))
                    .willReturn(Optional.of(sampleOrder)); // sampleOrder userId=42

            // WHEN & THEN: userId=99 başkasının siparişine erişmeye çalışıyor
            assertThatThrownBy(() -> orderService.getOrder("ORD-20260501-00001", 99L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("yetkiniz yok");
        }

        @Test
        @DisplayName("Sipariş numarası bulunamadı → NotFoundException")
        void getOrder_notFound_throwsNotFoundException() {
            // GIVEN
            given(orderRepository.findByOrderNumber("ORD-YANLISH-00000"))
                    .willReturn(Optional.empty());

            // WHEN & THEN
            assertThatThrownBy(() -> orderService.getOrder("ORD-YANLISH-00000", 42L))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  cancelOrder() testleri
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("cancelOrder()")
    class CancelOrderTests {

        @Test
        @DisplayName("PENDING durumda iptal → başarılı, DB güncellenir, event yayınlanır")
        void cancelOrder_pendingOrder_success() {
            // GIVEN
            sampleOrder.setStatus(OrderStatus.PENDING);
            given(orderRepository.findByOrderNumber("ORD-20260501-00001"))
                    .willReturn(Optional.of(sampleOrder));

            // WHEN
            orderService.cancelOrder("ORD-20260501-00001", 42L);

            // THEN: cancelOrder repository metodu çağrıldı mı?
            then(orderRepository).should().cancelOrder(
                    eq("ORD-20260501-00001"),
                    eq(OrderStatus.CANCELLED),
                    anyString()
            );

            // İptal bildirimi gönderildi mi?
            then(rabbitTemplate).should(atLeastOnce()).convertAndSend(anyString(), anyString(), any(Object.class));
        }

        @Test
        @DisplayName("DELIVERED durumda iptal → BusinessException (isCancellable=false)")
        void cancelOrder_deliveredOrder_throwsBusinessException() {
            // GIVEN — Teslim edilmiş sipariş iptal edilemez
            sampleOrder.setStatus(OrderStatus.DELIVERED);
            given(orderRepository.findByOrderNumber("ORD-20260501-00001"))
                    .willReturn(Optional.of(sampleOrder));

            // WHEN & THEN
            assertThatThrownBy(() -> orderService.cancelOrder("ORD-20260501-00001", 42L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("iptal edilemez");

            // DB güncellenmemeli
            then(orderRepository).should(never()).cancelOrder(any(), any(), any());
        }

        @Test
        @DisplayName("Başkasının siparişini iptal etme → BusinessException")
        void cancelOrder_otherUser_throwsBusinessException() {
            // GIVEN
            sampleOrder.setStatus(OrderStatus.PENDING);
            given(orderRepository.findByOrderNumber("ORD-20260501-00001"))
                    .willReturn(Optional.of(sampleOrder));

            // WHEN & THEN: userId=99 userId=42'nin siparişini iptal etmeye çalışıyor
            assertThatThrownBy(() -> orderService.cancelOrder("ORD-20260501-00001", 99L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("yetkiniz yok");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Saga Adımları testleri
    // ═══════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Saga Adımları")
    class SagaTests {

        @Test
        @DisplayName("onStockReserved → PENDING → STOCK_RESERVED, ödeme eventi yayınlanır")
        void onStockReserved_updatesStatusAndPublishesPaymentEvent() {
            // GIVEN
            sampleOrder.getItems().forEach(item -> item.setStockReserved(false));
            given(orderRepository.findByOrderNumber("ORD-20260501-00001"))
                    .willReturn(Optional.of(sampleOrder));
            given(orderRepository.updateStatus("ORD-20260501-00001", OrderStatus.PENDING, OrderStatus.STOCK_RESERVED))
                    .willReturn(1); // 1 satır güncellendi

            // WHEN
            orderService.onStockReserved("ORD-20260501-00001", 10L);

            // THEN: ödeme eventi yayınlandı mı?
            then(rabbitTemplate).should(atLeastOnce()).convertAndSend(anyString(), anyString(), any(Object.class));
        }

        @Test
        @DisplayName("onStockReserved → status uyumsuz stale event → ödeme başlatılmaz")
        void onStockReserved_staleEvent_doesNothing() {
            // GIVEN — sipariş zaten başka statüde
            sampleOrder.setStatus(OrderStatus.CONFIRMED);
            given(orderRepository.findByOrderNumber("ORD-UNKNOWN-99999")).willReturn(Optional.of(sampleOrder));

            // WHEN
            orderService.onStockReserved("ORD-UNKNOWN-99999", 10L);

            // THEN: rabbitTemplate çağrılmaz
            then(rabbitTemplate).should(never()).convertAndSend(anyString(), anyString(), any(Object.class));
        }

        @Test
        @DisplayName("onStockFailed → sipariş CANCELLED yapılır, bildirim gönderilir")
        void onStockFailed_cancelsOrderAndNotifies() {
            // GIVEN
            given(orderRepository.findByOrderNumber("ORD-20260501-00001"))
                    .willReturn(Optional.of(sampleOrder));

            // WHEN
            orderService.onStockFailed("ORD-20260501-00001", "Stok yetersiz");

            // THEN
            then(orderRepository).should().cancelOrder(
                    eq("ORD-20260501-00001"),
                    eq(OrderStatus.CANCELLED),
                    contains("Stok yetersiz")
            );
            then(rabbitTemplate).should(atLeastOnce()).convertAndSend(anyString(), anyString(), any(Object.class));
        }

        @Test
        @DisplayName("onCargoDelivered → SHIPPED → DELIVERED, notification + recommendation yayınlanır")
        void onCargoDelivered_setsDeliveredAndPublishesTwoEvents() {
            // GIVEN
            given(orderRepository.setDelivered("ORD-20260501-00001", OrderStatus.SHIPPED, OrderStatus.DELIVERED))
                    .willReturn(1);
            given(orderRepository.findByOrderNumber("ORD-20260501-00001"))
                    .willReturn(Optional.of(sampleOrder));

            // WHEN
            orderService.onCargoDelivered("ORD-20260501-00001");

            // THEN: en az 2 event: notification + recommendation
            then(rabbitTemplate).should(atLeast(2)).convertAndSend(anyString(), anyString(), any(Object.class));
        }
    }
}
