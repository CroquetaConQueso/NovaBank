package com.novabank.exception;

/**
 * Excepción base de negocio para NovaBank.
 *
 * Permite unificar el tratamiento de errores funcionales sin depender
 * directamente de IllegalArgumentException en toda la aplicación.
 */
public class NovaBankException extends RuntimeException {

    public NovaBankException(String message) {
        super(message);
    }

    public NovaBankException(String message, Throwable cause) {
        super(message, cause);
    }
}