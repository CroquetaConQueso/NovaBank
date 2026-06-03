package com.novabank.operacion.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OperacionAceptadaResponseDTO(
        UUID operationId,
        String estado,
        String mensaje,
        String tipoOperacion,
        Long cuentaId,
        BigDecimal importe
) {
}
