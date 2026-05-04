package com.novabank.dto;

import java.time.Instant;
import java.util.Map;

public record ErrorResponseDTO(
        String code,
        String message,
        Instant timestamp,
        Map<String, String> fieldErrors
) {

    public static ErrorResponseDTO of(String code, String message) {
        return new ErrorResponseDTO(code, message, Instant.now(), null);
    }

    public static ErrorResponseDTO withFieldErrors(
            String code,
            String message,
            Map<String, String> fieldErrors
    ) {
        return new ErrorResponseDTO(code, message, Instant.now(), fieldErrors);
    }
}
