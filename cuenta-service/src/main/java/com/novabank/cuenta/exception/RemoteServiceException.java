package com.novabank.cuenta.exception;

public class RemoteServiceException extends NovaBankException {

    public RemoteServiceException(String message) {
        super(message);
    }

    public RemoteServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
