package com.novabank.operacion.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public record OperacionAceptadaResult(
        UUID operationId,
        String estado,
        String mensaje,
        String tipoOperacion,
        Long cuentaId,
        BigDecimal importe
) {
}
