package com.novabank.exchangerate.dto;

import java.time.Instant;

public record ErrorResponseDTO(
        String code,
        String message,
        String service,
        Instant timestamp,
        String correlationId
) {
    public static ErrorResponseDTO of(String code, String message) {
        return of(code, message, null);
    }

    public static ErrorResponseDTO of(String code, String message, String correlationId) {
        return new ErrorResponseDTO(code, message, "exchange-rate-mock-service", Instant.now(), correlationId);
    }
}
