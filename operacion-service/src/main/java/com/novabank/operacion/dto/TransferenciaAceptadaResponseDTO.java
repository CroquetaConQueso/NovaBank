package com.novabank.operacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Respuesta 202 Accepted para transferencia ordinaria procesada de forma asincrona.")
public record TransferenciaAceptadaResponseDTO(
        @Schema(description = "Identificador de operacion asincrona para consultar /api/operaciones/sagas/{operationId}")
        UUID operationId,
        @Schema(example = "SOLICITADA")
        String estado,
        String mensaje,
        @Schema(example = "TRANSFERENCIA")
        String tipoOperacion,
        Long cuentaOrigenId,
        Long cuentaDestinoId,
        BigDecimal importe,
        String moneda
) {
}
