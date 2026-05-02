package com.ecommerce.order.service;

import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.order.dto.request.CreateOrderRequest;
import com.ecommerce.order.dto.response.OrderResponse;

public interface OrderService {
    OrderResponse createOrder(Long userId, CreateOrderRequest request);
    OrderResponse getOrder(String orderNumber, Long userId);
    PageResponse<OrderResponse> getUserOrders(Long userId, int page, int size);
    void cancelOrder(String orderNumber, Long userId);

    void onStockReserved(String orderNumber, Long productId);
    void onStockFailed(String orderNumber, String reason);
    void onPaymentSuccess(String orderNumber, String paymentIntentId);
    void onPaymentFailed(String orderNumber, String reason);
    void onCargoCreated(String orderNumber, String trackingNumber, String provider);
    void onCargoDelivered(String orderNumber);
}
