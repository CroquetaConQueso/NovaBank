package com.novabank.documento.application.exception;

public class DocumentoStorageException extends RuntimeException {

    public DocumentoStorageException(String message) {
        super(message);
    }

    public DocumentoStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
