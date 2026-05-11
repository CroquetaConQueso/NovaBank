package com.novabank.operacion.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponseDTO(
        String code,
        String message,
        String service,
        LocalDateTime timestamp,
        String correlationId,
        Map<String, String> fieldErrors
) {

    private static final String SERVICE_NAME = "operacion-service";

    public static ErrorResponseDTO of(String code, String message) {
        return of(code, message, null);
    }

    public static ErrorResponseDTO of(String code, String message, String correlationId) {
        return new ErrorResponseDTO(code, message, SERVICE_NAME, LocalDateTime.now(), correlationId, null);
    }

    public static ErrorResponseDTO withFieldErrors(
            String code,
            String message,
            String correlationId,
            Map<String, String> fieldErrors
    ) {
        return new ErrorResponseDTO(code, message, SERVICE_NAME, LocalDateTime.now(), correlationId, fieldErrors);
    }

    public static ErrorResponseDTO withFieldErrors(
            String code,
            String message,
            Map<String, String> fieldErrors
    ) {
        return withFieldErrors(code, message, null, fieldErrors);
    }
}
