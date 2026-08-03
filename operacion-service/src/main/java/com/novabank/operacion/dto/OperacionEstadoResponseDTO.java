package com.novabank.operacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Estado persistido de una operacion asincrona/SAGA del Modulo 6.")
public record OperacionEstadoResponseDTO(
        UUID operationId,
        UUID correlationId,
        @Schema(example = "DEPOSITO")
        String tipoOperacion,
        @Schema(example = "COMPLETADA", allowableValues = {"SOLICITADA", "COMPLETADA", "FALLIDA"})
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
