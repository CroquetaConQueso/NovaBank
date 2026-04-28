package com.novabank.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CuentaResponseDTO(
        Long id,
        String numeroCuenta,
        Long clienteId,
        BigDecimal saldo,
        LocalDateTime fechaCreacion
) {
}
