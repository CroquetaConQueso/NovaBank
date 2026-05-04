package com.novabank.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CuentaCreateRequestDTO(
        @NotNull(message = "El id del cliente es obligatorio")
        @Positive(message = "El id del cliente debe ser positivo")
        Long clienteId
) {
}
