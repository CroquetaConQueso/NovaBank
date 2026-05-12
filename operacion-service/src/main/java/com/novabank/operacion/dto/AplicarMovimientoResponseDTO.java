package com.novabank.operacion.dto;

public record AplicarMovimientoResponseDTO(
        String operationId,
        String estado,
        String mensaje,
        CuentaResponseDTO cuentaOrigen,
        CuentaResponseDTO cuentaDestino
) {
}
