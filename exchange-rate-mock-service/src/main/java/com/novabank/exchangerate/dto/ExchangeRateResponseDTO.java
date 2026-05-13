package com.novabank.exchangerate.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

public record ExchangeRateResponseDTO(
        @Schema(description = "Moneda origen", example = "USD")
        String from,

        @Schema(description = "Moneda destino", example = "EUR")
        String to,

        @Schema(description = "Tasa de cambio mock positiva", example = "0.92")
        BigDecimal tasa,

        Instant timestamp
) {
}
