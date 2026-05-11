package com.novabank.cuenta.dto;

import java.time.Instant;
import java.util.Map;

public record ErrorResponseDTO(
        String code,
        String message,
        String service,
        Instant timestamp,
        String correlationId,
        Map<String, String> fieldErrors
) {

    private static final String SERVICE_NAME = "cuenta-service";

    public static ErrorResponseDTO of(String code, String message) {
        return of(code, message, null);
    }

    public static ErrorResponseDTO of(String code, String message, String correlationId) {
        return new ErrorResponseDTO(code, message, SERVICE_NAME, Instant.now(), correlationId, null);
    }

    public static ErrorResponseDTO withFieldErrors(
            String code,
            String message,
            String correlationId,
            Map<String, String> fieldErrors
    ) {
        return new ErrorResponseDTO(code, message, SERVICE_NAME, Instant.now(), correlationId, fieldErrors);
    }

    public static ErrorResponseDTO withFieldErrors(
            String code,
            String message,
            Map<String, String> fieldErrors
    ) {
        return withFieldErrors(code, message, null, fieldErrors);
    }
}
