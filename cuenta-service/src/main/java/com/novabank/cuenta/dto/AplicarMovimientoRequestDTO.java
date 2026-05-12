package com.novabank.cuenta.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record AplicarMovimientoRequestDTO(
        @NotBlank(message = "El identificador de operacion es obligatorio")
        String operationId,

        @NotNull(message = "La cuenta origen es obligatoria")
        @Positive(message = "La cuenta origen debe ser positiva")
        Long cuentaOrigenId,

        @NotNull(message = "La cuenta destino es obligatoria")
        @Positive(message = "La cuenta destino debe ser positiva")
        Long cuentaDestinoId,

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor que cero")
        BigDecimal monto,

        String concepto
) {
}
