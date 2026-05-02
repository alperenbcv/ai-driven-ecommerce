package com.ecommerce.assistant.service;

import org.springframework.stereotype.Component;

@Component
public class UserContextHolder {

    private final ThreadLocal<Long> currentUserId = new ThreadLocal<>();

    public void setUserId(Long userId) {
        currentUserId.set(userId);
    }

    public Long getUserId() {
        return currentUserId.get();
    }

    public void clear() {
        currentUserId.remove();
    }
}
