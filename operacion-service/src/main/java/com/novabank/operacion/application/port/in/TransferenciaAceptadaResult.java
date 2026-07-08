package com.novabank.operacion.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferenciaAceptadaResult(
        UUID operationId,
        String estado,
        String mensaje,
        String tipoOperacion,
        Long cuentaOrigenId,
        Long cuentaDestinoId,
        BigDecimal importe,
        String moneda
) {
}
