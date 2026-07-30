package com.novabank.operacion.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public record SolicitarTransferenciaCommand(
        Long cuentaOrigenId,
        Long cuentaDestinoId,
        BigDecimal cantidad,
        String idempotencyKey,
        UUID correlationId,
        Boolean internacional,
        String paisDestino,
        String tipoCliente
) {

    public SolicitarTransferenciaCommand(
            Long cuentaOrigenId,
            Long cuentaDestinoId,
            BigDecimal cantidad,
            String idempotencyKey,
            UUID correlationId
    ) {
        this(cuentaOrigenId, cuentaDestinoId, cantidad, idempotencyKey, correlationId, false, null, null);
    }

    public boolean esInternacional() {
        return Boolean.TRUE.equals(internacional);
    }
}
