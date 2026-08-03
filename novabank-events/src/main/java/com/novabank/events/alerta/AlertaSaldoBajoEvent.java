package com.novabank.events.alerta;

import com.novabank.events.core.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AlertaSaldoBajoEvent(
        UUID eventId,
        UUID correlationId,
        Instant occurredAt,
        Long cuentaId,
        BigDecimal saldoActual,
        BigDecimal umbral,
        String moneda
) implements DomainEvent {
}
