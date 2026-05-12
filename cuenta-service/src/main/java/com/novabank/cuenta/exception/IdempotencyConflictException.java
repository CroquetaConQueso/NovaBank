package com.novabank.cuenta.exception;

public class IdempotencyConflictException extends NovaBankException {

    public IdempotencyConflictException(String message) {
        super(message);
    }
}
