package com.novabank.persistence.repository;

import com.novabank.domain.model.Cliente;

import java.util.List;

/**
 * Contrato de persistencia para clientes.
 */
public interface ClienteRepository {

    void anadirCliente(Cliente nuevoCliente);

    Cliente buscarIdCliente(Long idBusqueda);

    Cliente buscarDniCliente(String dniNif);

    Cliente buscarEmailCliente(String email);

    Cliente buscarTelefonoCliente(int telefonoCli);

    List<Cliente> obtenerClientes();
}