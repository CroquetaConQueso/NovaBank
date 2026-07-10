package com.novabank.documento.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record GenerarJustificanteOperacionCommand(
        UUID eventId,
        UUID correlationId,
        Instant occurredAt,
        UUID operationId,
        String tipoOperacion,
        Long cuentaOrigenId,
        Long cuentaDestinoId,
        Long cuentaId,
        Long movimientoId,
        BigDecimal importe,
        String moneda
) {
}
