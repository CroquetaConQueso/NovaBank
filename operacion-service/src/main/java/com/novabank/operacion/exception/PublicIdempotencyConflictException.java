package com.novabank.operacion.exception;

public class PublicIdempotencyConflictException extends NovaBankException {

    public PublicIdempotencyConflictException(String message) {
        super(message);
    }
}
