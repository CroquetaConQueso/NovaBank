package com.novabank.operacion.exception;

public class RemoteServiceException extends NovaBankException {

    public RemoteServiceException(String message) {
        super(message);
    }

    public RemoteServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
