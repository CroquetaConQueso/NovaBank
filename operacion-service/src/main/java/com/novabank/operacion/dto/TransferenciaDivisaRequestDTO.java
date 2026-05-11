package com.novabank.operacion.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransferenciaDivisaRequestDTO(
        @NotNull(message = "La cuenta origen es obligatoria")
        @Positive(message = "La cuenta origen debe ser positiva")
        Long cuentaOrigenId,

        @NotNull(message = "La cuenta destino es obligatoria")
        @Positive(message = "La cuenta destino debe ser positiva")
        Long cuentaDestinoId,

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor que cero")
        BigDecimal monto,

        @NotBlank(message = "La moneda origen es obligatoria")
        String monedaOrigen,

        @NotBlank(message = "La moneda destino es obligatoria")
        String monedaDestino
) {
}
