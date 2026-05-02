package com.ecommerce.notification.exception;

/**
 * SMTP/template hatası — Rabbit retry/DLQ zincirinin tetiklenebilmesi için taşınır.
 */
public class EmailSendException extends RuntimeException {

    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
