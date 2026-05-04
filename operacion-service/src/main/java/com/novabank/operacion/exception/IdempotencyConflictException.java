package com.novabank.operacion.exception;

public class IdempotencyConflictException extends NovaBankException {

    public IdempotencyConflictException(String message) {
        super(message);
    }
}
