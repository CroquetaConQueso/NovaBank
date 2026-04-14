package com.novabank.persistence.memory;

import com.novabank.domain.model.Cliente;
import com.novabank.persistence.repository.ClienteRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repositorio en memoria para clientes.
 */
public class RepositorioCliente implements ClienteRepository {

    private final Map<Long, Cliente> registroClientes = new HashMap<>();

    @Override
    public void anadirCliente(Cliente nuevoCliente) {
        registroClientes.put(nuevoCliente.getIdCliente(), nuevoCliente);
    }

    @Override
    public Optional<Cliente> buscarIdCliente(Long idBusqueda) {
        return Optional.ofNullable(registroClientes.get(idBusqueda));
    }

    @Override
    public Optional<Cliente> buscarDniCliente(String dniNif) {
        return registroClientes.values()
                .stream()
                .filter(cliente -> cliente.getDniNifCliente().equalsIgnoreCase(dniNif))
                .findFirst();
    }

    @Override
    public Optional<Cliente> buscarEmailCliente(String email) {
        return registroClientes.values()
                .stream()
                .filter(cliente -> cliente.getEmailCliente().equalsIgnoreCase(email))
                .findFirst();
    }

    @Override
    public Optional<Cliente> buscarTelefonoCliente(int telefonoCli) {
        return registroClientes.values()
                .stream()
                .filter(cliente -> cliente.getTelefonoCliente() == telefonoCli)
                .findFirst();
    }

    @Override
    public List<Cliente> obtenerClientes() {
        return registroClientes.values().stream().toList();
    }
}