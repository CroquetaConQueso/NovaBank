package com.novabank.cuenta.exception;

import com.novabank.cuenta.dto.ErrorResponseDTO;
import com.novabank.cuenta.tracing.CorrelationIdSupport;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
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

    @ExceptionHandler(InsufficientBalanceException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleInsufficientBalance(InsufficientBalanceException ex) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_BALANCE", ex.getMessage());
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleIdempotencyConflict(IdempotencyConflictException ex) {
        return response(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", ex.getMessage());
    }

    @ExceptionHandler(RemoteServiceException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleRemoteService(RemoteServiceException ex) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, "CLIENTE_SERVICE_UNAVAILABLE", ex.getMessage());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleOptimisticLocking(OptimisticLockingFailureException ex) {
        return response(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "La cuenta fue modificada por otra operacion. Vuelve a intentarlo.");
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

    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatusCode status = ex.getStatusCode();
        String message = ex.getReason() != null ? ex.getReason() : resolveStatusMessage(status);
        return response(status, resolveStatusCode(status), message);
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleGeneric(Exception ex) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "Se ha producido un error inesperado");
    }

    private Mono<ResponseEntity<ErrorResponseDTO>> response(HttpStatusCode status, String code, String message) {
        return Mono.deferContextual(contextView -> Mono.just(ResponseEntity.status(status)
                .body(ErrorResponseDTO.of(code, message, CorrelationIdSupport.fromContext(contextView)))));
    }

    private String resolveStatusCode(HttpStatusCode status) {
        HttpStatus httpStatus = HttpStatus.resolve(status.value());
        return httpStatus != null ? httpStatus.name() : "HTTP_" + status.value();
    }

    private String resolveStatusMessage(HttpStatusCode status) {
        HttpStatus httpStatus = HttpStatus.resolve(status.value());
        return httpStatus != null ? httpStatus.getReasonPhrase() : "HTTP " + status.value();
    }
}
