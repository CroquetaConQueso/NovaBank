package com.novabank.operacion.service;

import java.math.BigDecimal;

public record ExchangeRateQuote(
        BigDecimal tasa,
        boolean cacheada
) {
}
