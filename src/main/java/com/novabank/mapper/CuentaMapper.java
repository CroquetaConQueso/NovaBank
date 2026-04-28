package com.novabank.mapper;

import com.novabank.dto.CuentaResponseDTO;
import com.novabank.mapper.contract.ResponseMapper;
import com.novabank.model.Cuenta;
import org.springframework.stereotype.Component;

@Component
public class CuentaMapper implements ResponseMapper<Cuenta, CuentaResponseDTO> {

    @Override
    public CuentaResponseDTO toResponse(Cuenta cuenta) {
        Long clienteId = cuenta.getCliente() == null ? null : cuenta.getCliente().getId();

        return new CuentaResponseDTO(
                cuenta.getId(),
                cuenta.getNumeroCuenta(),
                clienteId,
                cuenta.getSaldo(),
                cuenta.getFechaCreacion()
        );
    }
}
