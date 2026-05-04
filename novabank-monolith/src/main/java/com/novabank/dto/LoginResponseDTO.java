package com.novabank.dto;

public record LoginResponseDTO(
        String token,
        String tipo,
        long expiracion
) {
}
