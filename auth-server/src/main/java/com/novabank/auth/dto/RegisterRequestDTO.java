package com.novabank.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO(
        @NotBlank(message = "El usuario es obligatorio")
        @Size(max = 80, message = "El usuario no puede superar 80 caracteres")
        String username,

        @NotBlank(message = "La password es obligatoria")
        @Size(min = 6, message = "La password debe tener al menos 6 caracteres")
        String password
) {
}
