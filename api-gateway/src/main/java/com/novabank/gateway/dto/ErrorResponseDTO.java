package com.novabank.gateway.dto;

import java.time.LocalDateTime;

public record ErrorResponseDTO(
        String code,
        String message,
        String service,
        LocalDateTime timestamp,
        String correlationId
) {

    private static final String SERVICE_NAME = "api-gateway";

    public static ErrorResponseDTO of(String code, String message) {
        return of(code, message, null);
    }

    public static ErrorResponseDTO of(String code, String message, String correlationId) {
        return new ErrorResponseDTO(code, message, SERVICE_NAME, LocalDateTime.now(), correlationId);
    }
}
