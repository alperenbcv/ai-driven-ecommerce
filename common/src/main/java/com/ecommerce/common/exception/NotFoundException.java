package com.ecommerce.common.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends BaseException {

    public NotFoundException(String resource, Object id) {
        super(resource + " bulunamadı: " + id, "NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    public NotFoundException(String message) {
        super(message, "NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
