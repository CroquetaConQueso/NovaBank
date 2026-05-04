package com.novabank.mapper;

import com.novabank.dto.ClienteRequestDTO;
import com.novabank.dto.ClienteResponseDTO;
import com.novabank.mapper.contract.RequestMapper;
import com.novabank.mapper.contract.ResponseMapper;
import com.novabank.model.Cliente;
import org.springframework.stereotype.Component;

@Component
public class ClienteMapper implements ResponseMapper<Cliente, ClienteResponseDTO>, RequestMapper<ClienteRequestDTO, Cliente> {

    @Override
    public Cliente toEntity(ClienteRequestDTO dto) {
        return Cliente.builder()
                .nombre(dto.nombre())
                .apellidos(dto.apellidos())
                .dni(dto.dni())
                .email(dto.email())
                .telefono(dto.telefono())
                .build();
    }

    @Override
    public ClienteResponseDTO toResponse(Cliente cliente) {
        int numeroCuentas = cliente.getCuentas() == null ? 0 : cliente.getCuentas().size();

        return new ClienteResponseDTO(
                cliente.getId(),
                cliente.getNombre(),
                cliente.getApellidos(),
                cliente.getDni(),
                cliente.getEmail(),
                cliente.getTelefono(),
                cliente.getFechaCreacion(),
                numeroCuentas
        );
    }
}
