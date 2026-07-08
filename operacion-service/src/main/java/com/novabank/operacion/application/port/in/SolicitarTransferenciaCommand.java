package com.novabank.operacion.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public record SolicitarTransferenciaCommand(
        Long cuentaOrigenId,
        Long cuentaDestinoId,
        BigDecimal cantidad,
        String idempotencyKey,
        UUID correlationId
) {
}
