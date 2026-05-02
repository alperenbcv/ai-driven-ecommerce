package com.ecommerce.order.service;

import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.NotFoundException;
import com.ecommerce.order.client.ProductCatalogClient;
import com.ecommerce.order.config.RabbitMQConfig;
import com.ecommerce.order.dto.request.CreateOrderRequest;
import com.ecommerce.order.dto.response.OrderResponse;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ProductCatalogClient productCatalogClient;

    private final AtomicLong orderCounter = new AtomicLong(0);

    @jakarta.annotation.PostConstruct
    void initOrderCounter() {
        orderRepository.findMaxOrderSequence()
                .ifPresentOrElse(
                    max -> { orderCounter.set(max + 1); log.info("Sipariş counter başlatıldı: {}", max + 1); },
                    ()  -> { orderCounter.set(1);       log.info("Sipariş counter başlatıldı: 1 (ilk sipariş)"); }
                );
    }

    private String generateOrderNumber() {
        String date = DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now());
        long seq = orderCounter.getAndIncrement();
        return "ORD-%s-%05d".formatted(date, seq);
    }
    @Override
    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        List<ValidatedOrderItem> validatedItems = request.getItems().stream()
                .map(this::validateAndPriceItem)
                .toList();

        BigDecimal totalAmount = validatedItems.stream()
                .map(ValidatedOrderItem::totalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CreateOrderRequest.ShippingAddressRequest addr = request.getShippingAddress();
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .userId(userId)
                .userEmail(request.getUserEmail())
                .totalAmount(totalAmount)
                .shippingFullName(addr.getFullName())
                .shippingPhone(addr.getPhone())
                .shippingCity(addr.getCity())
                .shippingDistrict(addr.getDistrict())
                .shippingFullAddress(addr.getFullAddress())
                .build();

        validatedItems.forEach(i -> {
            OrderItem item = OrderItem.builder()
                    .productId(i.productId())
                    .listingId(i.listingId())
                    .sellerId(i.sellerId())
                    .productName(i.productName())
                    .unitPrice(i.unitPrice())
                    .quantity(i.quantity())
                    .totalPrice(i.totalPrice())
                    .build();
            order.addItem(item);
        });

        Order saved = orderRepository.save(order);
        log.info("Sipariş oluşturuldu: orderNumber={}, userId={}, total={}",
                saved.getOrderNumber(), userId, totalAmount);

        publishStockReserveEvents(saved);
        publishNotificationEvent(saved, RabbitMQConfig.ORDER_CREATED_KEY, null, null);

        return toResponse(saved);
    }

    @Override
    public OrderResponse getOrder(String orderNumber, Long userId) {
        Order order = findByOrderNumber(orderNumber);
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("Bu siparişe erişim yetkiniz yok");
        }
        return toResponse(order);
    }

    @Override
    public PageResponse<OrderResponse> getUserOrders(Long userId, int page, int size) {
        Page<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return PageResponse.of(orders.map(this::toResponse));
    }

    @Override
    @Transactional
    public void cancelOrder(String orderNumber, Long userId) {
        Order order = findByOrderNumber(orderNumber);

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("Bu siparişe erişim yetkiniz yok");
        }
        if (!order.isCancellable()) {
            throw new BusinessException("Bu aşamadaki sipariş iptal edilemez. İade talebi oluşturun.");
        }

        orderRepository.cancelOrder(orderNumber, OrderStatus.CANCELLED, "Kullanıcı tarafından iptal edildi");

        if (order.getStatus() == OrderStatus.STOCK_RESERVED) {
            publishStockReleaseEvents(order);
        }

        publishNotificationEvent(order, RabbitMQConfig.ORDER_CANCELLED_KEY, "Kullanıcı tarafından iptal edildi", null);
        log.info("Sipariş iptal edildi: orderNumber={}", orderNumber);
    }

    @Override
    @Transactional
    public void onStockReserved(String orderNumber, Long productId) {
        Order order = findByOrderNumber(orderNumber);
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.STOCK_RESERVED) {
            log.warn("Stok rezerve eventi işlenemedi (status uyumsuz): orderNumber={}, status={}",
                    orderNumber, order.getStatus());
            return;
        }

        OrderItem item = order.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "Stok eventi siparişte olmayan ürün için geldi: productId=" + productId));

        if (!item.isStockReserved()) {
            item.setStockReserved(true);
            orderRepository.save(order);
        }

        boolean allReserved = order.getItems().stream().allMatch(OrderItem::isStockReserved);
        if (!allReserved) {
            log.info("Stok kısmen rezerve edildi: orderNumber={}, productId={}", orderNumber, productId);
            return;
        }

        int updated = orderRepository.updateStatus(orderNumber, OrderStatus.PENDING, OrderStatus.STOCK_RESERVED);
        if (updated == 0 && order.getStatus() != OrderStatus.STOCK_RESERVED) {
            log.warn("Stok rezerve status güncellenemedi: {}", orderNumber);
            return;
        }

        log.info("Tüm stoklar rezerve edildi, ödeme başlatılıyor: {}", orderNumber);
        publishPaymentInitiateEvent(findByOrderNumber(orderNumber));
    }

    @Override
    @Transactional
    public void onStockFailed(String orderNumber, String reason) {
        orderRepository.cancelOrder(orderNumber, OrderStatus.CANCELLED,
                "Stok yetersiz: " + reason);

        Order order = findByOrderNumber(orderNumber);
        publishStockReleaseEvents(order, true);
        publishNotificationEvent(order, RabbitMQConfig.ORDER_CANCELLED_KEY, "Stok yetersiz: " + reason, null);
        log.info("Sipariş stok yetersizliği nedeniyle iptal edildi: {}", orderNumber);
    }

    @Override
    @Transactional
    public void onPaymentSuccess(String orderNumber, String paymentIntentId) {
        int updated = orderRepository.setPaymentIntent(
                orderNumber, paymentIntentId, OrderStatus.CONFIRMED);

        if (updated == 0) {
            log.warn("Ödeme başarı eventi işlenemedi: {}", orderNumber);
            return;
        }

        log.info("Ödeme başarılı, sipariş onaylandı: {}", orderNumber);
        Order order = findByOrderNumber(orderNumber);

        publishStockConfirmEvents(order);
        publishCargoCreateEvent(order);
    }

    @Override
    @Transactional
    public void onPaymentFailed(String orderNumber, String reason) {
        orderRepository.cancelOrder(orderNumber, OrderStatus.CANCELLED,
                "Ödeme başarısız: " + reason);

        Order order = findByOrderNumber(orderNumber);
        publishStockReleaseEvents(order);
        publishNotificationEvent(order, RabbitMQConfig.ORDER_CANCELLED_KEY, "Ödeme başarısız: " + reason, null);

        log.info("Ödeme başarısız, sipariş iptal edildi: {}", orderNumber);
    }

    @Override
    @Transactional
    public void onCargoCreated(String orderNumber, String trackingNumber, String provider) {
        orderRepository.setCargoInfo(orderNumber, trackingNumber, provider, OrderStatus.SHIPPED);
        log.info("Kargo oluşturuldu: orderNumber={}, tracking={}", orderNumber, trackingNumber);

        Order order = findByOrderNumber(orderNumber);
        publishNotificationEvent(order, RabbitMQConfig.ORDER_SHIPPED_KEY, null, trackingNumber);
    }

    @Override
    @Transactional
    public void onCargoDelivered(String orderNumber) {
        int updated = orderRepository.setDelivered(orderNumber, OrderStatus.SHIPPED, OrderStatus.DELIVERED);

        if (updated == 0) {
            log.warn("Teslim eventi işlenemedi (sipariş bulunamadı veya status uyumsuz): {}", orderNumber);
            return;
        }

        log.info("Sipariş teslim edildi: {}", orderNumber);
        Order order = findByOrderNumber(orderNumber);

        publishNotificationEvent(order, RabbitMQConfig.ORDER_DELIVERED_NOTIFICATION_KEY, null, null);
        publishOrderDeliveredForRecommendation(order);
    }

    private void publishStockReserveEvents(Order order) {
        order.getItems().forEach(item -> {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("orderId", order.getOrderNumber());
                payload.put("productId", item.getProductId());
                payload.put("amount", item.getQuantity());
                if (item.getSellerId() != null) payload.put("sellerId", item.getSellerId());
                rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_RESERVE_KEY, payload);
            } catch (Exception e) {
                log.error("Stok rezerve eventi gönderilemedi: {}", e.getMessage());
            }
        });
    }

    private void publishStockReleaseEvents(Order order) {
        publishStockReleaseEvents(order, false);
    }

    private void publishStockReleaseEvents(Order order, boolean onlyReservedItems) {
        order.getItems().stream()
                .filter(item -> !onlyReservedItems || item.isStockReserved())
                .forEach(item -> {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("orderId", order.getOrderNumber());
                payload.put("productId", item.getProductId());
                payload.put("amount", item.getQuantity());
                if (item.getSellerId() != null) payload.put("sellerId", item.getSellerId());
                rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_RELEASE_KEY, payload);
            } catch (Exception e) {
                log.error("Stok iade eventi gönderilemedi: {}", e.getMessage());
            }
        });
    }

    private void publishStockConfirmEvents(Order order) {
        order.getItems().forEach(item -> {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("orderId", order.getOrderNumber());
                payload.put("productId", item.getProductId());
                payload.put("amount", item.getQuantity());
                if (item.getSellerId() != null) payload.put("sellerId", item.getSellerId());
                rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_CONFIRM_KEY, payload);
            } catch (Exception e) {
                log.error("Stok onay eventi gönderilemedi: {}", e.getMessage());
            }
        });
    }

    private void publishPaymentInitiateEvent(Order order) {
        try {
            List<Map<String, Object>> itemList = order.getItems().stream()
                    .map(i -> Map.<String, Object>of(
                        "name", i.getProductName(),
                        "price", i.getUnitPrice().toString(),
                        "quantity", i.getQuantity()
                    ))
                    .toList();

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.PAYMENT_INITIATE_KEY,
                Map.of(
                    "orderId", order.getOrderNumber(),
                    "userId", order.getUserId(),
                    "amount", order.getTotalAmount().toString(),
                    "items", itemList,
                    "buyerName", order.getShippingFullName(),
                    "buyerPhone", order.getShippingPhone()
                )
            );
        } catch (Exception e) {
            log.error("Ödeme başlatma eventi gönderilemedi: {}", e.getMessage());
        }
    }

    private void publishNotificationEvent(Order order, String routingKey, String cancelReason, String trackingNumber) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("orderId", order.getOrderNumber());
            payload.put("userEmail", order.getUserEmail() != null ? order.getUserEmail() : "");
            payload.put("totalAmount", order.getTotalAmount().toString());
            if (cancelReason != null) payload.put("cancelReason", cancelReason);
            if (trackingNumber != null) {
                payload.put("trackingNumber", trackingNumber);
                payload.put("provider", order.getCargoProvider() != null ? order.getCargoProvider() : "MockCargo");
            }
            rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, routingKey, payload);
        } catch (Exception e) {
            log.error("Notification eventi gönderilemedi: routingKey={}, hata={}", routingKey, e.getMessage());
        }
    }

    private void publishOrderDeliveredForRecommendation(Order order) {
        try {
            List<Map<String, Object>> itemList = order.getItems().stream()
                    .map(i -> Map.<String, Object>of(
                        "productId", i.getProductId(),
                        "name", i.getProductName()
                    ))
                    .toList();

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_DELIVERED_RECOMMENDATION_KEY,
                Map.of(
                    "orderId", order.getOrderNumber(),
                    "userId", order.getUserId(),
                    "userName", order.getShippingFullName(),
                    "items", itemList
                )
            );
        } catch (Exception e) {
            log.error("Recommendation eventi gönderilemedi: {}", e.getMessage());
        }
    }

    private void publishCargoCreateEvent(Order order) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.CARGO_CREATE_KEY,
                Map.of(
                    "orderId", order.getOrderNumber(),
                    "recipientName", order.getShippingFullName(),
                    "recipientPhone", order.getShippingPhone(),
                    "address", order.getShippingFullAddress(),
                    "city", order.getShippingCity(),
                    "district", order.getShippingDistrict()
                )
            );
        } catch (Exception e) {
            log.error("Kargo oluşturma eventi gönderilemedi: {}", e.getMessage());
        }
    }

    private ValidatedOrderItem validateAndPriceItem(CreateOrderRequest.OrderItemRequest item) {
        if (item.getQuantity() == null || item.getQuantity() <= 0) {
            throw new BusinessException("Ürün adedi 0'dan büyük olmalıdır");
        }

        if (item.getListingId() != null) {
            ProductCatalogClient.ProductListingSnapshot listing =
                    productCatalogClient.getListing(item.getProductId(), item.getListingId());

            if (!listing.isActive()) {
                throw new BusinessException("Seçilen ürün listing'i aktif değil: listingId=" + item.getListingId());
            }
            if (!item.getProductId().equals(listing.getProductId())) {
                throw new BusinessException("Listing seçilen ürüne ait değil");
            }

            BigDecimal total = listing.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            return new ValidatedOrderItem(
                    item.getProductId(),
                    item.getListingId(),
                    listing.getSellerId(),
                    listing.getProductName(),
                    listing.getPrice(),
                    item.getQuantity(),
                    total
            );
        }

        ProductCatalogClient.ProductSnapshot product = productCatalogClient.getProduct(item.getProductId());
        if (!product.isActive()) {
            throw new BusinessException("Ürün aktif değil: productId=" + item.getProductId());
        }

        BigDecimal total = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new ValidatedOrderItem(
                product.getId(),
                null,
                null,
                product.getName(),
                product.getPrice(),
                item.getQuantity(),
                total
        );
    }

    private Order findByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new NotFoundException("Sipariş bulunamadı: " + orderNumber));
    }

    private record ValidatedOrderItem(
            Long productId,
            Long listingId,
            Long sellerId,
            String productName,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal totalPrice
    ) {}

    private OrderResponse toResponse(Order order) {
        List<OrderResponse.OrderItemResponse> items = order.getItems().stream()
                .map(i -> OrderResponse.OrderItemResponse.builder()
                        .id(i.getId())
                        .productId(i.getProductId())
                        .productName(i.getProductName())
                        .unitPrice(i.getUnitPrice())
                        .quantity(i.getQuantity())
                        .totalPrice(i.getTotalPrice())
                        .sellerId(i.getSellerId())
                        .build())
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .shippingFullName(order.getShippingFullName())
                .shippingPhone(order.getShippingPhone())
                .shippingCity(order.getShippingCity())
                .shippingDistrict(order.getShippingDistrict())
                .shippingFullAddress(order.getShippingFullAddress())
                .cargoTrackingNumber(order.getCargoTrackingNumber())
                .cargoProvider(order.getCargoProvider())
                .cancelReason(order.getCancelReason())
                .items(items)
                .createdAt(order.getCreatedAt())
                .shippedAt(order.getShippedAt())
                .build();
    }
}
