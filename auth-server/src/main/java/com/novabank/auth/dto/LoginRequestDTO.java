package com.novabank.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciales de login. No incluir secretos reales en ejemplos compartidos.")
public record LoginRequestDTO(
        @NotBlank(message = "El usuario es obligatorio")
        @Schema(example = "usuario.demo")
        String username,

        @NotBlank(message = "La password es obligatoria")
        @Schema(example = "password-demo", format = "password")
        String password
) {
}
