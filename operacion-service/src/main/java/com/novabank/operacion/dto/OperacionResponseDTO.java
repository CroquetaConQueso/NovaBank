package com.novabank.operacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record OperacionResponseDTO(
        String tipoOperacion,

        @Schema(description = "Mensaje funcional; en divisa puede indicar uso de tasa cacheada")
        String mensaje,

        List<MovimientoResponseDTO> movimientos
) {
}
