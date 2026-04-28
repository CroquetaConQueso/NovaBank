package com.novabank.service;

import com.novabank.dto.ClienteRequestDTO;
import com.novabank.dto.ClienteResponseDTO;
import com.novabank.exception.DuplicateResourceException;
import com.novabank.exception.ResourceNotFoundException;
import com.novabank.exception.ValidationException;
import com.novabank.mapper.ClienteMapper;
import com.novabank.model.Cliente;
import com.novabank.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;

    public ClienteService(ClienteRepository clienteRepository, ClienteMapper clienteMapper) {
        this.clienteRepository = clienteRepository;
        this.clienteMapper = clienteMapper;
    }

    @Transactional
    public ClienteResponseDTO crearCliente(ClienteRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Los datos del cliente son obligatorios");
        }

        ClienteRequestDTO normalizado = normalizar(request);

        validarDuplicados(normalizado);

        Cliente cliente = clienteMapper.toEntity(normalizado);
        return clienteMapper.toResponse(clienteRepository.save(cliente));
    }

    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> listarClientes() {
        return clienteRepository.findAllWithCuentas()
                .stream()
                .map(clienteMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponseDTO obtenerCliente(Long id) {
        return clienteMapper.toResponse(buscarCliente(id));
    }

    @Transactional(readOnly = true)
    public ClienteResponseDTO obtenerClientePorDni(String dni) {
        if (dni == null || dni.isBlank()) {
            throw new ValidationException("El DNI es obligatorio");
        }

        String dniNormalizado = dni.trim().toUpperCase(Locale.ROOT);

        return clienteRepository.findByDni(dniNormalizado)
                .map(clienteMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No existe ningun cliente con DNI " + dniNormalizado));
    }

    Cliente buscarCliente(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El id del cliente debe ser positivo");
        }

        return clienteRepository.findByIdWithCuentas(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe ningun cliente con id " + id));
    }

    private ClienteRequestDTO normalizar(ClienteRequestDTO request) {
        return new ClienteRequestDTO(
                normalizarTexto(request.nombre(), "El nombre es obligatorio"),
                normalizarTexto(request.apellidos(), "Los apellidos son obligatorios"),
                normalizarTexto(request.dni(), "El DNI es obligatorio").toUpperCase(Locale.ROOT),
                normalizarTexto(request.email(), "El email es obligatorio").toLowerCase(Locale.ROOT),
                normalizarTexto(request.telefono(), "El telefono es obligatorio")
        );
    }

    private String normalizarTexto(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }

        return valor.trim();
    }

    private void validarDuplicados(ClienteRequestDTO request) {
        List<Cliente> duplicados = clienteRepository.buscarDuplicados(
                request.dni(),
                request.email(),
                request.telefono()
        );

        if (contiene(duplicados, c -> request.dni().equals(c.getDni()))) {
            throw new DuplicateResourceException("Ya existe un cliente con el DNI " + request.dni());
        }
        if (contiene(duplicados, c -> request.email().equals(c.getEmail()))) {
            throw new DuplicateResourceException("Ya existe un cliente con el email " + request.email());
        }
        if (contiene(duplicados, c -> request.telefono().equals(c.getTelefono()))) {
            throw new DuplicateResourceException("Ya existe un cliente con el telefono " + request.telefono());
        }
    }

    private boolean contiene(List<Cliente> clientes, Predicate<Cliente> predicate) {
        return clientes != null && clientes.stream().anyMatch(predicate);
    }
}
