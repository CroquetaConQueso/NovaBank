package com.novabank.dto;

import java.time.LocalDateTime;

public record ClienteResponseDTO(
        Long id,
        String nombre,
        String apellidos,
        String dni,
        String email,
        String telefono,
        LocalDateTime fechaCreacion,
        int numeroCuentas
) {
}
