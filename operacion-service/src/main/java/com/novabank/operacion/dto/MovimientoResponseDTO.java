package com.novabank.operacion.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoResponseDTO(
        Long id,
        Long cuentaId,
        String numeroCuenta,
        String tipo,
        BigDecimal cantidad,
        LocalDateTime fecha
) {
}
