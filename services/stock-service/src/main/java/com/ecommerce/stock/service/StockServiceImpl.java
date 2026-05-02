package com.ecommerce.stock.service;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.NotFoundException;
import com.ecommerce.stock.config.RabbitMQConfig;
import com.ecommerce.stock.dto.request.StockAdjustRequest;
import com.ecommerce.stock.dto.request.StockCreateRequest;
import com.ecommerce.stock.dto.response.StockResponse;
import com.ecommerce.stock.entity.Stock;
import com.ecommerce.stock.entity.StockMovement;
import com.ecommerce.stock.entity.StockMovement.MovementType;
import com.ecommerce.stock.repository.StockMovementRepository;
import com.ecommerce.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StockServiceImpl implements StockService {

    private final StockRepository stockRepository;
    private final StockMovementRepository movementRepository;
    private final RabbitTemplate rabbitTemplate;


    @Override
    @Transactional
    public StockResponse createOrUpdateStock(StockCreateRequest request) {
        Optional<Stock> existing = request.getSellerId() != null
                ? stockRepository.findByProductIdAndSellerId(request.getProductId(), request.getSellerId())
                : stockRepository.findByProductIdAndSellerIdIsNull(request.getProductId());

        Stock stock;
        if (existing.isPresent()) {
            stock = existing.get();
            stock.setQuantity(request.getInitialQuantity());
            stock.setLowStockThreshold(request.getLowStockThreshold());
            log.info("Stok güncellendi: productId={}, sellerId={}, qty={}",
                    request.getProductId(), request.getSellerId(), request.getInitialQuantity());
        } else {
            stock = Stock.builder()
                    .productId(request.getProductId())
                    .sellerId(request.getSellerId())
                    .quantity(request.getInitialQuantity())
                    .lowStockThreshold(request.getLowStockThreshold())
                    .build();
            log.info("Stok oluşturuldu: productId={}, sellerId={}, qty={}",
                    request.getProductId(), request.getSellerId(), request.getInitialQuantity());
        }

        Stock saved = stockRepository.save(stock);
        recordMovement(saved.getProductId(), saved.getSellerId(), MovementType.STOCK_IN,
                request.getInitialQuantity(), 0, saved.getQuantity(), null, "Başlangıç/güncelleme stoğu");
        return toResponse(saved);
    }

    @Override
    public StockResponse getStockByProductId(Long productId) {
        List<Stock> stocks = stockRepository.findByProductId(productId);
        if (stocks.isEmpty()) {
            throw new NotFoundException("Stok kaydı bulunamadı: productId=" + productId);
        }
        int totalQty = stocks.stream().mapToInt(Stock::getQuantity).sum();
        int totalReserved = stocks.stream().mapToInt(Stock::getReservedQty).sum();
        return StockResponse.builder()
                .productId(productId)
                .quantity(totalQty)
                .reservedQty(totalReserved)
                .availableQty(totalQty - totalReserved)
                .lowStockThreshold(stocks.get(0).getLowStockThreshold())
                .lowStock((totalQty - totalReserved) <= stocks.get(0).getLowStockThreshold())
                .build();
    }

    @Override
    public StockResponse getStockByProductIdAndSellerId(Long productId, Long sellerId) {
        Stock stock = findStock(productId, sellerId);
        return toResponse(stock);
    }

    @Override
    public List<StockResponse> getAllStocksForProduct(Long productId) {
        return stockRepository.findByProductId(productId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public StockResponse adjustStock(Long productId, Long sellerId, StockAdjustRequest request) {
        Stock stock = findStock(productId, sellerId);
        int before = stock.getQuantity();

        if (request.getQuantity() < stock.getReservedQty()) {
            throw new BusinessException(
                "Yeni stok miktarı rezerve miktarından (%d) az olamaz".formatted(stock.getReservedQty())
            );
        }

        stock.setQuantity(request.getQuantity());
        stockRepository.save(stock);

        int delta = request.getQuantity() - before;
        recordMovement(productId, sellerId, MovementType.ADJUSTMENT, delta, before, request.getQuantity(),
                null, request.getReason());

        log.info("Stok düzeltildi: productId={}, sellerId={}, {} → {}", productId, sellerId, before, request.getQuantity());
        return toResponse(stock);
    }

    @Override
    @Transactional
    public boolean reserveStock(Long productId, Long sellerId, int amount, String orderId) {
        ReserveTarget target = reserveOnStock(productId, sellerId, amount);
        Long effectiveSellerId = target.sellerIdForRow();

        if (target.updated() == 0) {
            log.warn("Stok rezervasyonu başarısız: productId={}, sellerId={}, amount={}, orderId={}",
                    productId, sellerId, amount, orderId);
            publishStockFailedEvent(orderId, productId, "Yetersiz stok");
            return false;
        }

        Stock stock = findStock(productId, effectiveSellerId);
        recordMovement(productId, sellerId, MovementType.RESERVE, -amount,
                stock.getQuantity(), stock.getQuantity(), orderId, null);

        if (stock.isLowStock()) {
            publishLowStockAlert(productId, stock.getAvailableQty());
        }

        log.info("Stok rezerve edildi: productId={}, orderSellerId={}, ledgerSellerId={}, amount={}, orderId={}",
                productId, sellerId, effectiveSellerId, amount, orderId);
        publishStockReservedEvent(orderId, productId, amount);
        return true;
    }

    @Override
    @Transactional
    public boolean releaseStock(Long productId, Long sellerId, int amount, String orderId) {
        ReleaseTarget target = releaseOnStock(productId, sellerId, amount);
        Long effectiveSellerId = target.sellerIdForRow();

        if (target.updated() == 0) {
            log.error("Rezervasyon iadesi başarısız: productId={}, sellerId={}, amount={}", productId, sellerId, amount);
            return false;
        }

        Stock stock = findStock(productId, effectiveSellerId);
        recordMovement(productId, sellerId, MovementType.RELEASE, amount,
                stock.getQuantity(), stock.getQuantity(), orderId, "Sipariş iptali");

        log.info("Rezervasyon iade edildi: productId={}, orderSellerId={}, ledgerSellerId={}, amount={}, orderId={}",
                productId, sellerId, effectiveSellerId, amount, orderId);
        return true;
    }

    @Override
    @Transactional
    public boolean confirmStock(Long productId, Long sellerId, int amount, String orderId) {
        ConfirmTarget target = confirmOnStock(productId, sellerId, amount);
        Long effectiveSellerId = target.sellerIdForRow();

        if (target.updated() == 0) {
            log.error("Stok onayı başarısız: productId={}, sellerId={}, orderId={}", productId, sellerId, orderId);
            return false;
        }

        Stock stock = findStock(productId, effectiveSellerId);
        recordMovement(productId, sellerId, MovementType.CONFIRM, -amount,
                stock.getQuantity() + amount, stock.getQuantity(), orderId, "Ödeme onayı");

        log.info("Stok onaylandı: productId={}, orderSellerId={}, ledgerSellerId={}, amount={}, orderId={}",
                productId, sellerId, effectiveSellerId, amount, orderId);
        return true;
    }

    private ReserveTarget reserveOnStock(Long productId, Long sellerId, int amount) {
        if (sellerId == null) {
            return new ReserveTarget(stockRepository.reserveStockLegacy(productId, amount), null);
        }
        int updated = stockRepository.reserveStockForSeller(productId, sellerId, amount);
        if (updated == 0 && !stockRepository.existsByProductIdAndSellerId(productId, sellerId)) {
            updated = stockRepository.reserveStockLegacy(productId, amount);
            return new ReserveTarget(updated, updated > 0 ? null : sellerId);
        }
        return new ReserveTarget(updated, sellerId);
    }

    private ReleaseTarget releaseOnStock(Long productId, Long sellerId, int amount) {
        if (sellerId == null) {
            return new ReleaseTarget(stockRepository.releaseReservationLegacy(productId, amount), null);
        }
        int updated = stockRepository.releaseReservationForSeller(productId, sellerId, amount);
        if (updated == 0 && !stockRepository.existsByProductIdAndSellerId(productId, sellerId)) {
            updated = stockRepository.releaseReservationLegacy(productId, amount);
            return new ReleaseTarget(updated, updated > 0 ? null : sellerId);
        }
        return new ReleaseTarget(updated, sellerId);
    }

    private ConfirmTarget confirmOnStock(Long productId, Long sellerId, int amount) {
        if (sellerId == null) {
            return new ConfirmTarget(stockRepository.confirmReservationLegacy(productId, amount), null);
        }
        int updated = stockRepository.confirmReservationForSeller(productId, sellerId, amount);
        if (updated == 0 && !stockRepository.existsByProductIdAndSellerId(productId, sellerId)) {
            updated = stockRepository.confirmReservationLegacy(productId, amount);
            return new ConfirmTarget(updated, updated > 0 ? null : sellerId);
        }
        return new ConfirmTarget(updated, sellerId);
    }

    private record ReserveTarget(int updated, Long ledgerSellerId) {
        Long sellerIdForRow() {
            return ledgerSellerId;
        }
    }

    private record ReleaseTarget(int updated, Long ledgerSellerId) {
        Long sellerIdForRow() {
            return ledgerSellerId;
        }
    }

    private record ConfirmTarget(int updated, Long ledgerSellerId) {
        Long sellerIdForRow() {
            return ledgerSellerId;
        }
    }

    private Stock findStock(Long productId, Long sellerId) {
        if (sellerId != null) {
            return stockRepository.findByProductIdAndSellerId(productId, sellerId)
                    .orElseThrow(() -> new NotFoundException(
                            "Stok kaydı bulunamadı: productId=%d, sellerId=%d".formatted(productId, sellerId)));
        } else {
            return stockRepository.findByProductIdAndSellerIdIsNull(productId)
                    .orElseThrow(() -> new NotFoundException("Stok kaydı bulunamadı: productId=" + productId));
        }
    }

    private void recordMovement(Long productId, Long sellerId, MovementType type, int delta,
                                 int before, int after, String refId, String note) {
        movementRepository.save(StockMovement.builder()
                .productId(productId)
                .movementType(type)
                .delta(delta)
                .quantityBefore(before)
                .quantityAfter(after)
                .referenceId(refId)
                .note(note)
                .build());
    }

    private void publishStockReservedEvent(String orderId, Long productId, int amount) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.STOCK_EXCHANGE,
                RabbitMQConfig.STOCK_RESERVED_KEY,
                Map.of("orderId", orderId, "productId", productId, "amount", amount, "status", "RESERVED")
            );
        } catch (Exception e) {
            log.warn("STOCK_RESERVED event yayınlanamadı: {}", e.getMessage());
        }
    }

    private void publishStockFailedEvent(String orderId, Long productId, String reason) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.STOCK_EXCHANGE,
                RabbitMQConfig.STOCK_FAILED_KEY,
                Map.of("orderId", orderId, "productId", productId, "reason", reason, "status", "FAILED")
            );
        } catch (Exception e) {
            log.warn("STOCK_FAILED event yayınlanamadı: {}", e.getMessage());
        }
    }

    private void publishLowStockAlert(Long productId, int availableQty) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.STOCK_EXCHANGE,
                RabbitMQConfig.LOW_STOCK_KEY,
                Map.of("productId", productId, "availableQty", availableQty)
            );
        } catch (Exception e) {
            log.warn("LOW_STOCK event yayınlanamadı: {}", e.getMessage());
        }
    }

    private StockResponse toResponse(Stock stock) {
        return StockResponse.builder()
                .id(stock.getId())
                .productId(stock.getProductId())
                .sellerId(stock.getSellerId())
                .quantity(stock.getQuantity())
                .reservedQty(stock.getReservedQty())
                .availableQty(stock.getAvailableQty())
                .lowStockThreshold(stock.getLowStockThreshold())
                .lowStock(stock.isLowStock())
                .build();
    }
}
