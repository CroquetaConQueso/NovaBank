package com.novabank.auth.exception;

import com.novabank.auth.dto.ErrorResponseDTO;
import com.novabank.auth.tracing.CorrelationIdSupport;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
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
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateUserException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleDuplicate(DuplicateUserException ex) {
        return response(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return response(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", ex.getMessage());
    }

    @ExceptionHandler(InvalidTokenException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleInvalidToken(InvalidTokenException ex) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", ex.getMessage());
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

    @ExceptionHandler(ConstraintViolationException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> fieldErrors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));

        return Mono.deferContextual(contextView -> Mono.just(ResponseEntity.badRequest()
                .body(ErrorResponseDTO.withFieldErrors(
                        "VALIDATION_ERROR",
                        "La peticion contiene campos invalidos",
                        CorrelationIdSupport.fromContext(contextView),
                        fieldErrors
                ))));
    }

    @ExceptionHandler(ServerWebInputException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleBadRequest(ServerWebInputException ex) {
        return response(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "La peticion no contiene los datos requeridos");
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
