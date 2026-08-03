package com.novabank.events.cliente;

import com.novabank.events.core.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record ClienteRegistradoEvent(
        UUID eventId,
        UUID correlationId,
        Instant occurredAt,
        Long clienteId,
        String dni,
        String nombre,
        String email
) implements DomainEvent {
}
