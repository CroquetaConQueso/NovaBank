package com.novabank.gateway.dto;

public record ValidateTokenResponseDTO(
        boolean valido,
        String username
) {
}
