package com.novabank.operacion.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ExchangeRateResultDTO(
        BigDecimal tasa,
        boolean cacheada,
        Instant timestamp
) {
}
