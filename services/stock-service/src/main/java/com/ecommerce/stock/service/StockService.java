package com.ecommerce.stock.service;

import com.ecommerce.stock.dto.request.StockAdjustRequest;
import com.ecommerce.stock.dto.request.StockCreateRequest;
import com.ecommerce.stock.dto.response.StockResponse;

import java.util.List;

public interface StockService {
    StockResponse createOrUpdateStock(StockCreateRequest request);

    StockResponse getStockByProductId(Long productId);

    StockResponse getStockByProductIdAndSellerId(Long productId, Long sellerId);

    List<StockResponse> getAllStocksForProduct(Long productId);

    StockResponse adjustStock(Long productId, Long sellerId, StockAdjustRequest request);

    boolean reserveStock(Long productId, Long sellerId, int amount, String orderId);
    boolean releaseStock(Long productId, Long sellerId, int amount, String orderId);
    boolean confirmStock(Long productId, Long sellerId, int amount, String orderId);
}
