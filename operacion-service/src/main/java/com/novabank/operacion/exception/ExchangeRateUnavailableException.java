package com.novabank.operacion.exception;

public class ExchangeRateUnavailableException extends NovaBankException {

    public ExchangeRateUnavailableException(String message) {
        super(message);
    }

    public ExchangeRateUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
