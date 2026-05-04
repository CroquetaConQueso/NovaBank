package com.novabank.cuenta.exception;

public class NovaBankException extends RuntimeException {

    public NovaBankException(String message) {
        super(message);
    }

    public NovaBankException(String message, Throwable cause) {
        super(message, cause);
    }
}
