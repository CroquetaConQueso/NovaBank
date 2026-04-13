package com.novabank.exception;

/**
 * Se lanza cuando no se encuentra un recurso solicitado.
 */
public class ResourceNotFoundException extends NovaBankException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}