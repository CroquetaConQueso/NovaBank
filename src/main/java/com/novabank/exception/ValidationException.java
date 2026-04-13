package com.novabank.exception;

/**
 * Se lanza cuando un dato de entrada no cumple las reglas de validación.
 */
public class ValidationException extends NovaBankException {

    public ValidationException(String message) {
        super(message);
    }
}