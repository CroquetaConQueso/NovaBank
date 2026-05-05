package com.novabank.cliente.dto;

import java.time.Instant;
import java.util.Map;

public record ErrorResponseDTO(
        String code,
        String message,
        String service,
        Instant timestamp,
        Map<String, String> fieldErrors
) {

    private static final String SERVICE_NAME = "cliente-service";

    public static ErrorResponseDTO of(String code, String message) {
        return new ErrorResponseDTO(code, message, SERVICE_NAME, Instant.now(), null);
    }

    public static ErrorResponseDTO withFieldErrors(
            String code,
            String message,
            Map<String, String> fieldErrors
    ) {
        return new ErrorResponseDTO(code, message, SERVICE_NAME, Instant.now(), fieldErrors);
    }
}
