package com.novabank.documento.adapter.out.storage;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

record JustificanteJson(
        UUID operationId,
        UUID correlationId,
        String tipoOperacion,
        Long cuentaOrigenId,
        Long cuentaDestinoId,
        BigDecimal importe,
        String moneda,
        Instant occurredAt,
        Instant generadoEn
) {
}
