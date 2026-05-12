package com.novabank.cliente.mapper;

import com.novabank.cliente.dto.ClienteRequestDTO;
import com.novabank.cliente.dto.ClienteResponseDTO;
import com.novabank.cliente.mapper.contract.RequestMapper;
import com.novabank.cliente.mapper.contract.ResponseMapper;
import com.novabank.cliente.model.Cliente;
import org.springframework.stereotype.Component;

@Component
public class ClienteMapper implements ResponseMapper<Cliente, ClienteResponseDTO>, RequestMapper<ClienteRequestDTO, Cliente> {

    /**
     * Construye la entidad solo desde el contrato de entrada para evitar que la
     * API exponga detalles internos de persistencia.
     */
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

    public void updateEntityFromRequest(Cliente cliente, ClienteRequestDTO dto) {
        cliente.setNombre(dto.nombre());
        cliente.setApellidos(dto.apellidos());
        cliente.setDni(dto.dni());
        cliente.setEmail(dto.email());
        cliente.setTelefono(dto.telefono());
    }

    /**
     * Devuelve un contrato de salida estable sin exponer el modelo persistido al
     * controlador.
     */
    @Override
    public ClienteResponseDTO toResponse(Cliente cliente) {
        return new ClienteResponseDTO(
                cliente.getId(),
                cliente.getNombre(),
                cliente.getApellidos(),
                cliente.getDni(),
                cliente.getEmail(),
                cliente.getTelefono(),
                cliente.getFechaCreacion()
        );
    }
}
