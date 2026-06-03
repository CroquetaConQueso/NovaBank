package com.novabank.events.operacion;

import com.novabank.events.core.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record OperacionFallidaEvent(
        UUID eventId,
        UUID correlationId,
        Instant occurredAt,
        UUID operationId,
        String tipoOperacion,
        String codigoError,
        String motivo
) implements DomainEvent {
}
