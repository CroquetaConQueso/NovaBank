package com.novabank.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
        @NotBlank(message = "El usuario es obligatorio")
        String username,

        @NotBlank(message = "La password es obligatoria")
        String password
) {
}
