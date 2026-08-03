package com.novabank.operacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Respuesta 202 Accepted para operaciones simples procesadas de forma asincrona.")
public record OperacionAceptadaResponseDTO(
        @Schema(description = "Identificador de operacion asincrona para consultar /api/operaciones/sagas/{operationId}")
        UUID operationId,
        @Schema(example = "SOLICITADA")
        String estado,
        String mensaje,
        @Schema(example = "RETIRADA")
        String tipoOperacion,
        Long cuentaId,
        BigDecimal importe
) {
}
