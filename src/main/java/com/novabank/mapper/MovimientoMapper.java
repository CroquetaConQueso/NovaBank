package com.novabank.mapper;

import com.novabank.dto.MovimientoResponseDTO;
import com.novabank.mapper.contract.ResponseMapper;
import com.novabank.model.Cuenta;
import com.novabank.model.Movimiento;
import org.springframework.stereotype.Component;

@Component
public class MovimientoMapper implements ResponseMapper<Movimiento, MovimientoResponseDTO> {

    @Override
    public MovimientoResponseDTO toResponse(Movimiento movimiento) {
        Cuenta cuenta = movimiento.getCuenta();

        return new MovimientoResponseDTO(
                movimiento.getId(),
                cuenta == null ? null : cuenta.getId(),
                cuenta == null ? null : cuenta.getNumeroCuenta(),
                movimiento.getTipo(),
                movimiento.getCantidad(),
                movimiento.getFecha()
        );
    }
}
