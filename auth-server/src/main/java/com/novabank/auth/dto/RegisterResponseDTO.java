package com.novabank.auth.dto;

import java.time.LocalDateTime;

public record RegisterResponseDTO(
        Long id,
        String username,
        String role,
        boolean enabled,
        LocalDateTime fechaCreacion
) {
}
