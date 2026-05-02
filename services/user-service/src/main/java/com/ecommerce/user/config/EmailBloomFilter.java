package com.ecommerce.user.config;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailBloomFilter {

    static final String REDIS_KEY = "email:bloom:v1";

    private final UserRepository userRepository;
    private final RedisTemplate<String, byte[]> bloomRedisTemplate;

    private volatile BloomFilter<String> filter;

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        try {
            byte[] cached = bloomRedisTemplate.opsForValue().get(REDIS_KEY);

            if (cached != null && cached.length > 0) {
                try {
                    filter = BloomFilter.readFrom(
                        new ByteArrayInputStream(cached),
                        Funnels.stringFunnel(StandardCharsets.UTF_8)
                    );
                    log.info("EmailBloomFilter Redis'ten yüklendi ({} bayt)", cached.length);
                    return;
                } catch (IOException e) {
                    log.warn("Redis'teki BloomFilter okunamadı, DB'den yeniden oluşturuluyor: {}", e.getMessage());
                }
            }

            buildFromDbAndSave();
        } catch (Exception e) {
            log.warn("Redis erişilemedi, BloomFilter sadece memory'de tutulacak: {}", e.getMessage());
            buildFromDbOnly();
        }
    }


    public boolean mightExist(String email) {
        if (filter == null) return false;
        return filter.mightContain(email.toLowerCase());
    }

    public void add(String email) {
        if (filter == null) return;
        filter.put(email.toLowerCase());
        CompletableFuture.runAsync(this::saveToRedis);
    }

    private void buildFromDbAndSave() {
        buildFromDbOnly();
        saveToRedis();
    }

    private void buildFromDbOnly() {
        BloomFilter<String> newFilter = BloomFilter.create(
            Funnels.stringFunnel(StandardCharsets.UTF_8),
            100_000,
            0.01
        );

        long count = userRepository.findAllEmails()
                .stream()
                .peek(e -> newFilter.put(e.toLowerCase()))
                .count();

        filter = newFilter;
        log.info("EmailBloomFilter DB'den oluşturuldu: {} e-posta", count);
    }

    private void saveToRedis() {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            filter.writeTo(bos);
            bloomRedisTemplate.opsForValue().set(REDIS_KEY, bos.toByteArray());
            log.debug("EmailBloomFilter Redis'e yazıldı ({} bayt)", bos.size());
        } catch (IOException e) {
            log.warn("BloomFilter Redis'e yazılamadı: {}", e.getMessage());
        }
    }

}
