package com.novabank.operacion.dto;

import java.math.BigDecimal;

public record AplicarMovimientoRequestDTO(
        String operationId,
        Long cuentaOrigenId,
        Long cuentaDestinoId,
        BigDecimal monto,
        String concepto
) {
}
