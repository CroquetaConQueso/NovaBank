package com.novabank.operacion.exception;

import com.novabank.operacion.application.exception.ComisionNoDisponibleException;
import com.novabank.operacion.dto.ErrorResponseDTO;
import com.novabank.operacion.tracing.CorrelationIdSupport;
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

    @ExceptionHandler(RemoteResourceNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleRemoteNotFound(RemoteResourceNotFoundException ex) {
        return response(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(OperacionAsincronaNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleOperacionAsincronaNotFound(OperacionAsincronaNotFoundException ex) {
        return response(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(RemoteValidationException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleRemoteValidation(RemoteValidationException ex) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, "REMOTE_VALIDATION_ERROR", ex.getMessage());
    }

    @ExceptionHandler(RemoteConflictException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleRemoteConflict(RemoteConflictException ex) {
        return response(HttpStatus.CONFLICT, "REMOTE_CONFLICT", ex.getMessage());
    }

    @ExceptionHandler(PublicIdempotencyConflictException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handlePublicIdempotencyConflict(PublicIdempotencyConflictException ex) {
        return response(HttpStatus.CONFLICT, "PUBLIC_IDEMPOTENCY_CONFLICT", ex.getMessage());
    }

    @ExceptionHandler(RemoteServiceException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleRemoteService(RemoteServiceException ex) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, "CUENTA_SERVICE_UNAVAILABLE", ex.getMessage());
    }

    @ExceptionHandler(ExchangeRateUnavailableException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleExchangeRateUnavailable(ExchangeRateUnavailableException ex) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, "EXCHANGE_RATE_UNAVAILABLE", ex.getMessage());
    }

    @ExceptionHandler(ComisionNoDisponibleException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleComisionNoDisponible(ComisionNoDisponibleException ex) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, "LAMBDA_COMISION_UNAVAILABLE", ex.getMessage());
    }

    @ExceptionHandler(EventoNoPublicadoException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleEventoNoPublicado(EventoNoPublicadoException ex) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, "KAFKA_EVENT_NOT_PUBLISHED", ex.getMessage());
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            ValidationException.class
    })
    public Mono<ResponseEntity<ErrorResponseDTO>> handleBadRequest(RuntimeException ex) {
        return response(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage());
    }

    @ExceptionHandler(ServerWebInputException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleUnreadableMessage(ServerWebInputException ex) {
        return response(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "La peticion contiene un JSON invalido");
    }

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
