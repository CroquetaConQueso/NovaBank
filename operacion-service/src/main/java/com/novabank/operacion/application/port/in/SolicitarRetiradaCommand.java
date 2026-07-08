package com.novabank.operacion.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public record SolicitarRetiradaCommand(
        Long cuentaId,
        BigDecimal cantidad,
        String idempotencyKey,
        UUID correlationId
) {
}
