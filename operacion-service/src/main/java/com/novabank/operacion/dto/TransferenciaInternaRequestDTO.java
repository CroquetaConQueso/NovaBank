package com.novabank.operacion.dto;

import java.math.BigDecimal;

public record TransferenciaInternaRequestDTO(
        Long cuentaOrigenId,
        Long cuentaDestinoId,
        BigDecimal cantidad
) {
}
