package com.novabank.events.alerta;

import com.novabank.events.core.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record AlertaOperacionSospechosaEvent(
        UUID eventId,
        UUID correlationId,
        Instant occurredAt,
        Long cuentaId,
        String tipoOperacion,
        Long numeroOperaciones,
        Long ventanaMinutos,
        String descripcion
) implements DomainEvent {
}
