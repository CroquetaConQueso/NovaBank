package com.novabank.cliente.exception;

public class EventoNoPublicadoException extends NovaBankException {

    public EventoNoPublicadoException(String message) {
        super(message);
    }

    public EventoNoPublicadoException(String message, Throwable cause) {
        super(message, cause);
    }
}
