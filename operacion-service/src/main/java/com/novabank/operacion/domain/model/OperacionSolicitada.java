package com.novabank.operacion.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OperacionSolicitada(
        UUID eventId,
        UUID correlationId,
        Instant occurredAt,
        UUID operationId,
        String tipoOperacion,
        Long cuentaOrigenId,
        Long cuentaDestinoId,
        Long cuentaId,
        BigDecimal importe,
        String moneda,
        Long kafkaKey
) {
}
