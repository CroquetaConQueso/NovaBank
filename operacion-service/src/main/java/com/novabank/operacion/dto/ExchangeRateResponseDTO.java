package com.novabank.operacion.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ExchangeRateResponseDTO(
        String from,
        String to,
        BigDecimal tasa,
        Instant timestamp
) {
}
