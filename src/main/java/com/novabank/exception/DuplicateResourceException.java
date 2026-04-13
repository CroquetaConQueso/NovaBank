package com.novabank.exception;

/**
 * Se lanza cuando se intenta registrar un recurso que ya existe.
 */
public class DuplicateResourceException extends NovaBankException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}