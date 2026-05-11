package com.novabank.cliente.service;

import com.novabank.cliente.dto.ClienteRequestDTO;
import com.novabank.cliente.dto.ClienteResponseDTO;
import com.novabank.cliente.exception.DuplicateResourceException;
import com.novabank.cliente.exception.ResourceNotFoundException;
import com.novabank.cliente.exception.ValidationException;
import com.novabank.cliente.mapper.ClienteMapper;
import com.novabank.cliente.model.Cliente;
import com.novabank.cliente.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;

    public ClienteService(ClienteRepository clienteRepository, ClienteMapper clienteMapper) {
        this.clienteRepository = clienteRepository;
        this.clienteMapper = clienteMapper;
    }

    /**
     * Normaliza los datos antes de comprobar unicidad para que DNI, email y
     * telefono se comparen con el mismo criterio que se persiste.
     */
    public Mono<ClienteResponseDTO> crearCliente(ClienteRequestDTO request) {
        return Mono.defer(() -> {
            if (request == null) {
                return Mono.error(new IllegalArgumentException("Los datos del cliente son obligatorios"));
            }

            ClienteRequestDTO normalizado = normalizar(request);

            return validarDuplicados(normalizado)
                    .then(Mono.fromSupplier(() -> {
                        Cliente cliente = clienteMapper.toEntity(normalizado);
                        cliente.prepararParaCreacion();
                        return cliente;
                    }))
                    .flatMap(clienteRepository::save)
                    .map(clienteMapper::toResponse);
        });
    }

    /**
     * Excluye el propio cliente al comprobar duplicados para permitir guardar
     * una actualizacion que conserva DNI, email o telefono.
     */
    public Mono<ClienteResponseDTO> actualizarCliente(Long id, ClienteRequestDTO request) {
        return Mono.defer(() -> {
            if (request == null) {
                return Mono.error(new IllegalArgumentException("Los datos del cliente son obligatorios"));
            }

            ClienteRequestDTO normalizado = normalizar(request);

            return buscarCliente(id)
                    .flatMap(cliente -> validarDuplicados(id, normalizado)
                            .then(Mono.defer(() -> {
                                clienteMapper.updateEntityFromRequest(cliente, normalizado);
                                return clienteRepository.save(cliente);
                            })))
                    .map(clienteMapper::toResponse);
        });
    }

    public Flux<ClienteResponseDTO> listarClientes() {
        return clienteRepository.findAll()
                .map(clienteMapper::toResponse);
    }

    public Mono<ClienteResponseDTO> obtenerCliente(Long id) {
        return buscarCliente(id)
                .map(clienteMapper::toResponse);
    }

    /**
     * Aplica la misma normalizacion que el alta para evitar busquedas fallidas
     * por espacios o diferencias de mayusculas.
     */
    public Mono<ClienteResponseDTO> obtenerClientePorDni(String dni) {
        return Mono.defer(() -> {
            if (dni == null || dni.isBlank()) {
                return Mono.error(new ValidationException("El DNI es obligatorio"));
            }

            String dniNormalizado = dni.trim().toUpperCase(Locale.ROOT);

            return clienteRepository.findByDni(dniNormalizado)
                    .map(clienteMapper::toResponse)
                    .switchIfEmpty(Mono.error(
                            new ResourceNotFoundException("No existe ningun cliente con DNI " + dniNormalizado)
                    ));
        });
    }

    Mono<Cliente> buscarCliente(Long id) {
        return Mono.defer(() -> {
            if (id == null || id <= 0) {
                return Mono.error(new IllegalArgumentException("El id del cliente debe ser positivo"));
            }

            return clienteRepository.findById(id)
                    .switchIfEmpty(Mono.error(
                            new ResourceNotFoundException("No existe ningun cliente con id " + id)
                    ));
        });
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

    private Mono<Void> validarDuplicados(ClienteRequestDTO request) {
        return clienteRepository.buscarDuplicados(
                        request.dni(),
                        request.email(),
                        request.telefono()
                )
                .collectList()
                .flatMap(duplicados -> validarDuplicados(duplicados, request));
    }

    private Mono<Void> validarDuplicados(Long clienteId, ClienteRequestDTO request) {
        return clienteRepository.buscarDuplicadosExcluyendoId(
                        clienteId,
                        request.dni(),
                        request.email(),
                        request.telefono()
                )
                .collectList()
                .flatMap(duplicados -> validarDuplicados(duplicados, request));
    }

    private Mono<Void> validarDuplicados(List<Cliente> duplicados, ClienteRequestDTO request) {
        if (contiene(duplicados, c -> request.dni().equals(c.getDni()))) {
            return Mono.error(new DuplicateResourceException("Ya existe un cliente con el DNI " + request.dni()));
        }
        if (contiene(duplicados, c -> request.email().equals(c.getEmail()))) {
            return Mono.error(new DuplicateResourceException("Ya existe un cliente con el email " + request.email()));
        }
        if (contiene(duplicados, c -> request.telefono().equals(c.getTelefono()))) {
            return Mono.error(new DuplicateResourceException("Ya existe un cliente con el telefono " + request.telefono()));
        }

        return Mono.empty();
    }

    private boolean contiene(List<Cliente> clientes, Predicate<Cliente> predicate) {
        return clientes != null && clientes.stream().anyMatch(predicate);
    }
}
