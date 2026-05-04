package com.novabank.cliente.dto;

import java.time.Instant;
import java.util.Map;

public record ErrorResponseDTO(
        String code,
        String message,
        String service,
        String correlationId,
        Instant timestamp,
        Map<String, String> fieldErrors
) {

    private static final String SERVICE_NAME = "cliente-service";

    public static ErrorResponseDTO of(String code, String message, String correlationId) {
        return new ErrorResponseDTO(code, message, SERVICE_NAME, correlationId, Instant.now(), null);
    }

    public static ErrorResponseDTO withFieldErrors(
            String code,
            String message,
            String correlationId,
            Map<String, String> fieldErrors
    ) {
        return new ErrorResponseDTO(code, message, SERVICE_NAME, correlationId, Instant.now(), fieldErrors);
    }
}
