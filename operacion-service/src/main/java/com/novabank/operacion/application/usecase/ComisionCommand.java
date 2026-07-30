package com.novabank.operacion.application.usecase;

import java.math.BigDecimal;

public record ComisionCommand(
        BigDecimal importeEuros,
        String paisDestino,
        String tipoCliente
) {
}
