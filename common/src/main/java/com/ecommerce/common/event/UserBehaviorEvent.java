package com.ecommerce.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBehaviorEvent {

    public enum BehaviorType {
        VIEWED, ADDED_TO_CART, PURCHASED, SEARCHED
    }

    private Long userId;
    private Long productId;
    private BehaviorType behaviorType;
    private String searchQuery;

    @Builder.Default
    private LocalDateTime occurredAt = LocalDateTime.now();
}
