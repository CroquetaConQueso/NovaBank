package com.novabank.cuenta.exception;

public class InsufficientBalanceException extends NovaBankException {

    public InsufficientBalanceException(String message) {
        super(message);
    }
}
