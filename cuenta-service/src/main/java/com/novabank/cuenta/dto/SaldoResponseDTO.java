package com.novabank.cuenta.dto;

import java.math.BigDecimal;

public record SaldoResponseDTO(
        Long cuentaId,
        String numeroCuenta,
        BigDecimal saldo
) {
}
