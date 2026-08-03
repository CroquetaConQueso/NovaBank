package com.novabank.events.operacion;

import com.novabank.events.core.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OperacionCompletadaEvent(
        UUID eventId,
        UUID correlationId,
        Instant occurredAt,
        UUID operationId,
        String tipoOperacion,
        Long cuentaOrigenId,
        Long cuentaDestinoId,
        Long cuentaIdPrincipal,
        Long movimientoId,
        BigDecimal importe,
        String moneda
) implements DomainEvent {
}
