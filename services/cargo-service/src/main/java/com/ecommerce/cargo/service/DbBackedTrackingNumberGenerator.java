package com.ecommerce.cargo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Takip kodu sırasını DB'de saklar — restart sonrası çakışma olmaz.
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
