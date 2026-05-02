package com.ecommerce.cargo.repository;

import com.ecommerce.cargo.entity.Shipment;
import com.ecommerce.cargo.entity.Shipment.CargoStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findByTrackingNumber(String trackingNumber);
    Optional<Shipment> findByOrderNumber(String orderNumber);

    /**
     * Teslim edilmemiş aktif kargoları bul.
     * Scheduler bu listeyi alır ve durum güncellemesi yapar.
     */
    List<Shipment> findByStatusNotIn(List<CargoStatus> excludedStatuses);
}
