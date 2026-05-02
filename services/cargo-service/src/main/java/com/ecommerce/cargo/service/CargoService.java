package com.ecommerce.cargo.service;

import com.ecommerce.cargo.dto.TrackingResponse;

import java.util.Map;

public interface CargoService {

    void createShipment(Map<String, Object> event);

    void advanceShipmentStatuses();

    TrackingResponse track(String trackingNumber);

    TrackingResponse trackByOrder(String orderNumber);
}
