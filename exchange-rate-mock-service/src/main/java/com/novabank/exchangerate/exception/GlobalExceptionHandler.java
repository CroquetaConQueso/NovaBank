package com.novabank.exchangerate.exception;

import com.novabank.exchangerate.dto.ErrorResponseDTO;
import com.novabank.exchangerate.tracing.CorrelationIdSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
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

    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleResponseStatus(ResponseStatusException exception) {
        HttpStatusCode status = exception.getStatusCode();
        String message = exception.getReason() != null ? exception.getReason() : resolveStatusMessage(status);
        return response(status, resolveStatusCode(status), message);
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleUnexpected(Exception exception) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Error inesperado en exchange-rate-mock-service");
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
