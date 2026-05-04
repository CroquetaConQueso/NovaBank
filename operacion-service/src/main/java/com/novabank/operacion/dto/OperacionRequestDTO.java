package com.novabank.operacion.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OperacionRequestDTO(
        @NotNull(message = "La cuenta es obligatoria")
        @Positive(message = "La cuenta debe ser positiva")
        Long cuentaId,

        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor que cero")
        BigDecimal cantidad
) {
}
