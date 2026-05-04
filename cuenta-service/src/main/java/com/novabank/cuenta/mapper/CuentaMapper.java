package com.novabank.cuenta.mapper;

import com.novabank.cuenta.dto.CuentaResponseDTO;
import com.novabank.cuenta.mapper.contract.ResponseMapper;
import com.novabank.cuenta.model.Cuenta;
import org.springframework.stereotype.Component;

@Component
public class CuentaMapper implements ResponseMapper<Cuenta, CuentaResponseDTO> {

    @Override
    public CuentaResponseDTO toResponse(Cuenta cuenta) {
        return new CuentaResponseDTO(
                cuenta.getId(),
                cuenta.getNumeroCuenta(),
                cuenta.getClienteId(),
                cuenta.getSaldo(),
                cuenta.getFechaCreacion()
        );
    }
}
