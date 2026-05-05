package com.novabank.operacion.mapper;

import com.novabank.operacion.dto.MovimientoResponseDTO;
import com.novabank.operacion.model.Movimiento;
import org.springframework.stereotype.Component;

@Component
public class MovimientoMapper {

    public MovimientoResponseDTO toResponse(Movimiento movimiento) {
        if (movimiento == null) {
            return null;
        }

        return new MovimientoResponseDTO(
                movimiento.getId(),
                movimiento.getCuentaId(),
                movimiento.getNumeroCuenta(),
                movimiento.getTipo().name(),
                movimiento.getCantidad(),
                movimiento.getFecha()
        );
    }
}
