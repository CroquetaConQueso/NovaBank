package com.novabank.cuenta.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoEventDTO(
        Long cuentaId,
        Long movimientoId,

        @Schema(description = "Tipo de movimiento emitido por el stream SSE", example = "TRANSFERENCIA_SALIENTE")
        String tipo,

        BigDecimal monto,
        BigDecimal saldoResultante,
        String descripcion,
        LocalDateTime fecha,

        @Schema(description = "Identificador idempotente de la operacion que genero el evento, si existe")
        String operationId
) {
}
