package com.novabank.operacion.exception;

public class OperationAlreadyInProgressException extends NovaBankException {

    public OperationAlreadyInProgressException(String message) {
        super(message);
    }
}
