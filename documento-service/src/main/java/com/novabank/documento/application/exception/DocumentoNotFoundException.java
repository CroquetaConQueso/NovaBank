package com.novabank.documento.application.exception;

public class DocumentoNotFoundException extends RuntimeException {

    public DocumentoNotFoundException(String message) {
        super(message);
    }
}
