package com.novabank.cuenta.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoEventDTO(
        Long cuentaId,
        Long movimientoId,
        String tipo,
        BigDecimal monto,
        BigDecimal saldoResultante,
        String descripcion,
        LocalDateTime fecha,
        String operationId
) {
}
