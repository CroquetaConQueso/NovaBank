package com.novabank.operacion.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferenciaAceptadaResponseDTO(
        UUID operationId,
        String estado,
        String mensaje,
        String tipoOperacion,
        Long cuentaOrigenId,
        Long cuentaDestinoId,
        BigDecimal importe,
        String moneda
) {
}
