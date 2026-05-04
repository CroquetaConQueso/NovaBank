package com.novabank.operacion.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransferenciaRequestDTO(
        @NotNull(message = "La cuenta origen es obligatoria")
        @Positive(message = "La cuenta origen debe ser positiva")
        Long cuentaOrigenId,

        @NotNull(message = "La cuenta destino es obligatoria")
        @Positive(message = "La cuenta destino debe ser positiva")
        Long cuentaDestinoId,

        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor que cero")
        BigDecimal cantidad
) {
}
