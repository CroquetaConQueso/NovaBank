package com.novabank.exchangerate.dto;

import java.time.Instant;

public record ErrorResponseDTO(
        String code,
        String message,
        String service,
        Instant timestamp
) {
    public static ErrorResponseDTO of(String code, String message) {
        return new ErrorResponseDTO(code, message, "exchange-rate-mock-service", Instant.now());
    }
}
