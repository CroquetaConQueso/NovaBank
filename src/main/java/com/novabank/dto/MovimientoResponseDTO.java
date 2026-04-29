package com.novabank.dto;

import com.novabank.model.TipoMovimiento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoResponseDTO(
        Long id,
        Long cuentaId,
        String numeroCuenta,
        TipoMovimiento tipo,
        BigDecimal cantidad,
        LocalDateTime fecha
) {
}
