package com.novabank.persistence.memory;

import com.novabank.domain.model.Cliente;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repositorio en memoria para clientes.
 *
 * Su responsabilidad es almacenar y recuperar clientes, sin aplicar reglas
 * de negocio ni ocuparse de la presentación por consola.
 */
public class RepositorioCliente {

    private final Map<Long, Cliente> registroClientes = new HashMap<>();

    public void anadirCliente(Cliente nuevoCliente) {
        if (!registroClientes.containsKey(nuevoCliente.getIdCliente())) {
            registroClientes.put(nuevoCliente.getIdCliente(), nuevoCliente);
        }
    }

    public Cliente buscarIdCliente(Long idBusqueda) {
        return registroClientes.get(idBusqueda);
    }

    public Cliente buscarDniCliente(String dniNif) {
        return registroClientes.values()
                .stream()
                .filter(cliente -> cliente.getDniNifCliente().equalsIgnoreCase(dniNif))
                .findFirst()
                .orElse(null);
    }

    public Cliente buscarEmailCliente(String email) {
        return registroClientes.values()
                .stream()
                .filter(cliente -> cliente.getEmailCliente().equalsIgnoreCase(email))
                .findFirst()
                .orElse(null);
    }

    public Cliente buscarTelefonoCliente(int telefonoCli) {
        return registroClientes.values()
                .stream()
                .filter(cliente -> cliente.getTelefonoCliente() == telefonoCli)
                .findFirst()
                .orElse(null);
    }

    /**
     * Devuelve todos los clientes registrados en memoria.
     */
    public List<Cliente> obtenerClientes() {
        return registroClientes.values().stream().toList();
    }
}