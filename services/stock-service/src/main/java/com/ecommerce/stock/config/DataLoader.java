package com.ecommerce.stock.config;

import com.ecommerce.stock.entity.Stock;
import com.ecommerce.stock.entity.StockMovement;
import com.ecommerce.stock.repository.StockMovementRepository;
import com.ecommerce.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class DataLoader implements CommandLineRunner {

    static final long PRODUCT_COUNT = 74L;

    private final StockRepository stockRepository;
    private final StockMovementRepository movementRepository;

    @Override
    public void run(String... args) {
        if (stockRepository.count() > 0) {
            log.info("Stok verisi zaten mevcut, atlanıyor.");
            return;
        }

        Random rng = new Random(42);
        List<Stock> stocks = new ArrayList<>();
        List<StockMovement> movements = new ArrayList<>();

        for (long productId = 1; productId <= PRODUCT_COUNT; productId++) {
            int qty = computeInitialQty(productId, rng);
            int threshold = Math.max(3, qty / 10);

            addRow(stocks, movements, productId, null, qty, threshold);

            long sellerPrimary = 2L;
            addRow(stocks, movements, productId, sellerPrimary, qty, threshold);

            if (productId % 2 == 0) {
                long sellerAlternate = 3L;
                addRow(stocks, movements, productId, sellerAlternate, qty, threshold);
            }
        }

        stockRepository.saveAll(stocks);
        movementRepository.saveAll(movements);
        log.info("{} stok kaydı oluşturuldu (listing seller 2/3 + legacy).", stocks.size());
    }

    private static void addRow(List<Stock> stocks, List<StockMovement> movements,
                               long productId, Long sellerId, int qty, int threshold) {
        stocks.add(Stock.builder()
                .productId(productId)
                .sellerId(sellerId)
                .quantity(qty)
                .reservedQty(0)
                .lowStockThreshold(threshold)
                .build());

        String sellerNote = sellerId == null
                ? "Demo başlangıç stoğu (legacy)"
                : "Demo başlangıç stoğu (seller=%d)".formatted(sellerId);
        movements.add(StockMovement.builder()
                .productId(productId)
                .movementType(StockMovement.MovementType.STOCK_IN)
                .delta(qty)
                .quantityBefore(0)
                .quantityAfter(qty)
                .note(sellerNote)
                .build());
    }


    private int computeInitialQty(long id, Random rng) {
        return switch ((int) ((id - 1) / 12)) {
            case 0 -> 10 + rng.nextInt(25);
            case 1 -> 25 + rng.nextInt(75);
            case 2 -> 15 + rng.nextInt(30);
            case 3 -> 8 + rng.nextInt(20);
            case 4 -> 30 + rng.nextInt(100);
            case 5 -> 50 + rng.nextInt(150);
            case 6 -> 60 + rng.nextInt(120);
            default -> 5 + rng.nextInt(15);
        };
    }
}
