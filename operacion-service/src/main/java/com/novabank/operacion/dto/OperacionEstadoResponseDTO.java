package com.novabank.operacion.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OperacionEstadoResponseDTO(
        UUID operationId,
        UUID correlationId,
        String tipoOperacion,
        String estado,
        Long cuentaId,
        Long cuentaOrigenId,
        Long cuentaDestinoId,
        BigDecimal importe,
        String moneda,
        String motivoFallo,
        LocalDateTime creadaEn,
        LocalDateTime actualizadaEn
) {
}
