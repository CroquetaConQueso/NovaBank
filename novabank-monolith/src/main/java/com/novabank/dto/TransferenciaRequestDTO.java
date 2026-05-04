package com.novabank.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record TransferenciaRequestDTO(
        @NotBlank(message = "La cuenta origen es obligatoria")
        @Pattern(regexp = "ES\\d{20}", message = "La cuenta origen debe tener formato ES seguido de 20 digitos")
        String numeroCuentaOrigen,

        @NotBlank(message = "La cuenta destino es obligatoria")
        @Pattern(regexp = "ES\\d{20}", message = "La cuenta destino debe tener formato ES seguido de 20 digitos")
        String numeroCuentaDestino,

        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor que cero")
        BigDecimal cantidad
) {
}
