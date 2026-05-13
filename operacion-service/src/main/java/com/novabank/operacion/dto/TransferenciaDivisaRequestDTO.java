package com.novabank.operacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
        @Schema(description = "Importe expresado en la moneda origen", example = "100.00")
        BigDecimal monto,

        @NotBlank(message = "La moneda origen es obligatoria")
        @Schema(description = "Moneda origen en formato ISO 4217", example = "USD")
        String monedaOrigen,

        @NotBlank(message = "La moneda destino es obligatoria")
        @Schema(description = "Moneda destino en formato ISO 4217", example = "EUR")
        String monedaDestino
) {
}
