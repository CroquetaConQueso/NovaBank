package com.novabank.events.movimiento;

import com.novabank.events.core.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MovimientoRegistradoEvent(
        UUID eventId,
        UUID correlationId,
        Instant occurredAt,
        Long movimientoId,
        Long cuentaId,
        String tipoMovimiento,
        BigDecimal importe,
        BigDecimal saldoResultante,
        String moneda
) implements DomainEvent {
}
