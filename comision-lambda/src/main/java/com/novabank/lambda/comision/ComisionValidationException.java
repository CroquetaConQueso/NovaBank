package com.novabank.lambda.comision;

public class ComisionValidationException extends IllegalArgumentException {

    public ComisionValidationException(String message) {
        super(message);
    }
}
