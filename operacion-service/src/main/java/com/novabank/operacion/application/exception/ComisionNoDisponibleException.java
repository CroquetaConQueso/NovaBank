package com.novabank.operacion.application.exception;

public class ComisionNoDisponibleException extends RuntimeException {

    public ComisionNoDisponibleException(String message) {
        super(message);
    }

    public ComisionNoDisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
