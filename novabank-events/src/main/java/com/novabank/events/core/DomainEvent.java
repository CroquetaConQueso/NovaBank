package com.novabank.events.core;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {

    UUID eventId();

    UUID correlationId();

    Instant occurredAt();
}
