package com.novabank.operacion.dto;

import java.math.BigDecimal;

public record CuentaOperacionRequestDTO(
        BigDecimal cantidad
) {
}
