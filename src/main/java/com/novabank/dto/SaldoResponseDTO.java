package com.novabank.dto;

import java.math.BigDecimal;

public record SaldoResponseDTO(
        Long cuentaId,
        String numeroCuenta,
        BigDecimal saldo
) {
}
