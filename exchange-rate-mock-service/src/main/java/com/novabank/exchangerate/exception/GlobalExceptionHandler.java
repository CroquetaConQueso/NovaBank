package com.novabank.exchangerate.exception;

import com.novabank.exchangerate.dto.ErrorResponseDTO;
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
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponseDTO.of("EXCHANGE_RATE_NOT_FOUND", exception.getMessage())));
    }

    @ExceptionHandler({IllegalArgumentException.class, ServerWebInputException.class})
    public Mono<ResponseEntity<ErrorResponseDTO>> handleBadRequest(Exception exception) {
        return Mono.just(ResponseEntity.badRequest()
                .body(ErrorResponseDTO.of("BAD_REQUEST", exception.getMessage())));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleUnexpected(Exception exception) {
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponseDTO.of("INTERNAL_ERROR", "Error inesperado en exchange-rate-mock-service")));
    }
}
