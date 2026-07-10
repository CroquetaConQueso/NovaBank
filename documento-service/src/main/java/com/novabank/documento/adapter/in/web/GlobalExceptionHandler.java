package com.novabank.documento.adapter.in.web;

import com.novabank.documento.application.exception.DocumentoNotFoundException;
import com.novabank.documento.application.exception.DocumentoStorageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebInputException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DocumentoNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> documentoNoEncontrado(DocumentoNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "DOCUMENTO_NO_ENCONTRADO", exception.getMessage());
    }

    @ExceptionHandler(DocumentoStorageException.class)
    public ResponseEntity<ErrorResponseDTO> errorAlmacenamiento(DocumentoStorageException exception) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "DOCUMENTO_STORAGE_ERROR", exception.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, ServerWebInputException.class})
    public ResponseEntity<ErrorResponseDTO> errorValidacion(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "VALIDACION_ERROR", exception.getMessage());
    }

    private ResponseEntity<ErrorResponseDTO> error(HttpStatus status, String codigo, String mensaje) {
        return ResponseEntity.status(status)
                .body(new ErrorResponseDTO(codigo, mensaje, Instant.now()));
    }
}
