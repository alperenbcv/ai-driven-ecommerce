package com.ecommerce.cargo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cargo_tracking_sequences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CargoTrackingSequence {

    @Id
    @Column(name = "sequence_day", nullable = false, length = 8)
    private String sequenceDay;

    @Column(name = "last_value", nullable = false)
    private Long lastValue;
}
