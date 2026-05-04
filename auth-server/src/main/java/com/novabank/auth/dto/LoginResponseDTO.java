package com.novabank.auth.dto;

public record LoginResponseDTO(
        String token,
        String tipo,
        long expiracion
) {
}
