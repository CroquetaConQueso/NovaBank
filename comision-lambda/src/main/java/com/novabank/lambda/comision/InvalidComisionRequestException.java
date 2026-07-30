package com.novabank.lambda.comision;

public class InvalidComisionRequestException extends IllegalArgumentException {

    public InvalidComisionRequestException(String message) {
        super(message);
    }
}
