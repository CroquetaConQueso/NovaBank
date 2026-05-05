package com.novabank.operacion.dto;

import java.util.List;

public record OperacionResponseDTO(
        String tipoOperacion,
        String mensaje,
        List<MovimientoResponseDTO> movimientos
) {
}
