package com.novabank.persistence.repository;

import com.novabank.domain.model.Cliente;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia para clientes.
 */
public interface ClienteRepository {

    void anadirCliente(Cliente nuevoCliente);

    Optional<Cliente> buscarIdCliente(Long idBusqueda);

    Optional<Cliente> buscarDniCliente(String dniNif);

    Optional<Cliente> buscarEmailCliente(String email);

    Optional<Cliente> buscarTelefonoCliente(int telefonoCli);

    List<Cliente> obtenerClientes();
}