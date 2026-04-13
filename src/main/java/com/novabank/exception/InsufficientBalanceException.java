package com.novabank.exception;

/**
 * Se lanza cuando una operación requiere más saldo del disponible.
 */
public class InsufficientBalanceException extends NovaBankException {

    public InsufficientBalanceException(String message) {
        super(message);
    }
}
