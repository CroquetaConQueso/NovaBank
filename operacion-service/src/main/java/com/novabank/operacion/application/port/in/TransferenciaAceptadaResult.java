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
        String moneda,
        BigDecimal comision,
        BigDecimal tasaComision
) {

    public TransferenciaAceptadaResult(
            UUID operationId,
            String estado,
            String mensaje,
            String tipoOperacion,
            Long cuentaOrigenId,
            Long cuentaDestinoId,
            BigDecimal importe,
            String moneda
    ) {
        this(operationId, estado, mensaje, tipoOperacion, cuentaOrigenId, cuentaDestinoId, importe, moneda, null, null);
    }
}
