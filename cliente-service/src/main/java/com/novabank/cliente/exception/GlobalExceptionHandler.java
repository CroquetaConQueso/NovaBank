package com.novabank.cliente.exception;

import com.novabank.cliente.dto.ErrorResponseDTO;
import com.novabank.cliente.tracing.CorrelationIdSupport;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleNotFound(ResourceNotFoundException ex) {
        return response(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleDuplicateResource(DuplicateResourceException ex) {
        return response(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleDataIntegrity(DataIntegrityViolationException ex) {
        return response(HttpStatus.CONFLICT, "CONFLICT", "Ya existe un cliente con alguno de los datos unicos indicados");
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            ValidationException.class
    })
    public Mono<ResponseEntity<ErrorResponseDTO>> handleBadRequest(RuntimeException ex) {
        return response(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage());
    }

    @ExceptionHandler(ServerWebInputException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleWebInput(ServerWebInputException ex) {
        return response(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "La peticion contiene datos invalidos o JSON malformado");
    }

    /**
     * Conserva el primer error por campo para devolver una respuesta estable y
     * facil de comprobar desde clientes HTTP.
     */
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleValidation(WebExchangeBindException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }

        return Mono.deferContextual(contextView -> Mono.just(ResponseEntity.badRequest()
                .body(ErrorResponseDTO.withFieldErrors(
                        "VALIDATION_ERROR",
                        "La peticion contiene campos invalidos",
                        CorrelationIdSupport.fromContext(contextView),
                        fieldErrors
                ))));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleGeneric(Exception ex) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "Se ha producido un error inesperado");
    }

    private Mono<ResponseEntity<ErrorResponseDTO>> response(HttpStatus status, String code, String message) {
        return Mono.deferContextual(contextView -> Mono.just(ResponseEntity.status(status)
                .body(ErrorResponseDTO.of(code, message, CorrelationIdSupport.fromContext(contextView)))));
    }
}
