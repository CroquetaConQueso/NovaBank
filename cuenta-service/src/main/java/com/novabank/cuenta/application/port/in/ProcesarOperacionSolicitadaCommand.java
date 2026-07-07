package com.novabank.cuenta.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProcesarOperacionSolicitadaCommand(
        UUID eventId,
        UUID correlationId,
        Instant occurredAt,
        UUID operationId,
        String tipoOperacion,
        Long cuentaOrigenId,
        Long cuentaDestinoId,
        BigDecimal importe,
        String moneda
) {
}
