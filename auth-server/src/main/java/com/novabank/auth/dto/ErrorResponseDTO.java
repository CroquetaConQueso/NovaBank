package com.novabank.auth.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponseDTO(
        String code,
        String message,
        String service,
        String correlationId,
        LocalDateTime timestamp,
        Map<String, String> fieldErrors
) {

    private static final String SERVICE_NAME = "auth-server";

    public static ErrorResponseDTO of(String code, String message, String correlationId) {
        return new ErrorResponseDTO(code, message, SERVICE_NAME, correlationId, LocalDateTime.now(), null);
    }

    public static ErrorResponseDTO withFieldErrors(
            String code,
            String message,
            String correlationId,
            Map<String, String> fieldErrors
    ) {
        return new ErrorResponseDTO(code, message, SERVICE_NAME, correlationId, LocalDateTime.now(), fieldErrors);
    }
}
