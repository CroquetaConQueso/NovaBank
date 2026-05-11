package com.novabank.cuenta.exception;

import com.novabank.cuenta.dto.ErrorResponseDTO;
import org.springframework.dao.OptimisticLockingFailureException;
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
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponseDTO.of("RESOURCE_NOT_FOUND", ex.getMessage())));
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleInsufficientBalance(InsufficientBalanceException ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponseDTO.of("INSUFFICIENT_BALANCE", ex.getMessage())));
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleIdempotencyConflict(IdempotencyConflictException ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponseDTO.of("IDEMPOTENCY_CONFLICT", ex.getMessage())));
    }

    @ExceptionHandler(RemoteServiceException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleRemoteService(RemoteServiceException ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponseDTO.of("CLIENTE_SERVICE_UNAVAILABLE", ex.getMessage())));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleOptimisticLocking(OptimisticLockingFailureException ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponseDTO.of(
                        "CONCURRENT_MODIFICATION",
                        "La cuenta fue modificada por otra operacion. Vuelve a intentarlo."
                )));
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            ValidationException.class
    })
    public Mono<ResponseEntity<ErrorResponseDTO>> handleBadRequest(RuntimeException ex) {
        return Mono.just(ResponseEntity.badRequest()
                .body(ErrorResponseDTO.of("BAD_REQUEST", ex.getMessage())));
    }

    @ExceptionHandler(ServerWebInputException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleUnreadableMessage(ServerWebInputException ex) {
        return Mono.just(ResponseEntity.badRequest()
                .body(ErrorResponseDTO.of("BAD_REQUEST", "La peticion contiene un JSON invalido")));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleValidation(WebExchangeBindException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }

        return Mono.just(ResponseEntity.badRequest()
                .body(ErrorResponseDTO.withFieldErrors(
                        "VALIDATION_ERROR",
                        "La peticion contiene campos invalidos",
                        fieldErrors
                )));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleGeneric(Exception ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponseDTO.of(
                        "INTERNAL_SERVER_ERROR",
                        "Se ha producido un error inesperado"
                )));
    }
}
