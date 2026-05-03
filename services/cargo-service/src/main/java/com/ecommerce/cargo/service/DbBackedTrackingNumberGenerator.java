package com.ecommerce.cargo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Mock kargo takip numarası üretmek için kullanılan DB destekli sequence generator'dır.
 *
 * Normalde takip numarası sadece memory'de sayaç tutularak da üretilebilirdi.
 * Ancak uygulama yeniden başlatıldığında memory sıfırlanacağı için aynı takip
 * numarasının tekrar üretilme riski oluşurdu. Bu yüzden sıra bilgisi veritabanında
 * saklanır.
 *
 * Üretilen takip numarası formatı:
 *   MC-YYYYMMDD-00001

 *
 * ON CONFLICT kullanımı:
 * PostgreSQL tarafında aynı sequence_day için duplicate kayıt oluşmasını engeller.
 * Böylece aynı gün içinde her takip numarası atomik olarak artar ve çakışmaz.
 *
 * JdbcTemplate:
 * Burada basit ve doğrudan SQL çalıştırmak için kullanıldı. JPA entity/repository
 * açmak yerine tek bir UPSERT sorgusuyla sequence üretmek daha kısa ve kontrollü
 * olduğu için tercih edildi.
 */
@Component
@RequiredArgsConstructor
public class DbBackedTrackingNumberGenerator {

    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;

    private final JdbcTemplate jdbcTemplate;

    public String nextMockCargoNumber(LocalDate date) {
        String dayKey = DAY.format(date);
        Long seq = jdbcTemplate.queryForObject(
                """
                        INSERT INTO cargo_tracking_sequences (sequence_day, last_value)
                        VALUES (?, 1)
                        ON CONFLICT (sequence_day) DO UPDATE SET
                          last_value = cargo_tracking_sequences.last_value + 1
                        RETURNING last_value
                        """,
                Long.class,
                dayKey);
        if (seq == null) {
            throw new IllegalStateException("Tracking sequence UPSERT beklenmedik şekilde null döndü: " + dayKey);
        }
        return "MC-%s-%05d".formatted(dayKey, seq);
    }
}
