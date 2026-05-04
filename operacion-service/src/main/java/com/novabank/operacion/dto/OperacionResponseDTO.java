package com.novabank.operacion.dto;

import com.novabank.operacion.model.EstadoOperacion;
import com.novabank.operacion.model.TipoOperacion;

import java.util.List;

public record OperacionResponseDTO(
        String idempotencyKey,
        TipoOperacion tipoOperacion,
        EstadoOperacion estado,
        List<MovimientoResponseDTO> movimientos
) {
}
