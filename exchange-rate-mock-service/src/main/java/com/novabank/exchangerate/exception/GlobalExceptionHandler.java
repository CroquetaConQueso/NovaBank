package com.novabank.exchangerate.exception;

import com.novabank.exchangerate.dto.ErrorResponseDTO;
import com.novabank.exchangerate.tracing.CorrelationIdSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ExchangeRateNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleExchangeRateNotFound(ExchangeRateNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "EXCHANGE_RATE_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, ServerWebInputException.class})
    public Mono<ResponseEntity<ErrorResponseDTO>> handleBadRequest(Exception exception) {
        return response(HttpStatus.BAD_REQUEST, "BAD_REQUEST", exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleUnexpected(Exception exception) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Error inesperado en exchange-rate-mock-service");
    }

    private Mono<ResponseEntity<ErrorResponseDTO>> response(HttpStatus status, String code, String message) {
        return Mono.deferContextual(contextView -> Mono.just(ResponseEntity.status(status)
                .body(ErrorResponseDTO.of(code, message, CorrelationIdSupport.fromContext(contextView)))));
    }
}
