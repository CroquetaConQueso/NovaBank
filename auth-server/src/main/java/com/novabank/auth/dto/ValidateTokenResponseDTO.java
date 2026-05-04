package com.novabank.auth.dto;

public record ValidateTokenResponseDTO(
        boolean valido,
        String username
) {
}
