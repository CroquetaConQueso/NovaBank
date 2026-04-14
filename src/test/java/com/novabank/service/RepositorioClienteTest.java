package com.novabank.service;

import com.novabank.domain.model.Cliente;
import com.novabank.persistence.memory.RepositorioCliente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RepositorioClienteTest {

    private RepositorioCliente repositorio;

    @BeforeEach
    void setUp() {
        repositorio = new RepositorioCliente();
    }

    private Cliente crearClienteEjemplo() {
        return Cliente.builder()
                .nombreCliente("Carlos")
                .apellidosCliente("Torres")
                .dniNifCliente("12345678A")
                .emailCliente("carlos@email.com")
                .telefonoCliente(600123123)
                .fechaCreacionCliente(LocalDateTime.now())
                .build();
    }

    @Test
    void debeAnadirClienteCorrectamente() {
        Cliente cliente = crearClienteEjemplo();

        repositorio.anadirCliente(cliente);

        Optional<Cliente> resultado = repositorio.buscarIdCliente(cliente.getIdCliente());

        assertTrue(resultado.isPresent());
        assertEquals(cliente.getDniNifCliente(), resultado.get().getDniNifCliente());
    }

    @Test
    void noDebeSobrescribirClienteConMismoId() {
        Cliente cliente = crearClienteEjemplo();
        repositorio.anadirCliente(cliente);

        repositorio.anadirCliente(cliente);

        Optional<Cliente> resultado = repositorio.buscarIdCliente(cliente.getIdCliente());

        assertTrue(resultado.isPresent());
        assertEquals(cliente.getEmailCliente(), resultado.get().getEmailCliente());
    }

    @Test
    void debeBuscarPorDni() {
        Cliente cliente = crearClienteEjemplo();
        repositorio.anadirCliente(cliente);

        Optional<Cliente> resultado = repositorio.buscarDniCliente("12345678A");

        assertTrue(resultado.isPresent());
        assertEquals(cliente.getIdCliente(), resultado.get().getIdCliente());
    }

    @Test
    void debeBuscarPorEmail() {
        Cliente cliente = crearClienteEjemplo();
        repositorio.anadirCliente(cliente);

        Optional<Cliente> resultado = repositorio.buscarEmailCliente("carlos@email.com");

        assertTrue(resultado.isPresent());
        assertEquals(cliente.getEmailCliente(), resultado.get().getEmailCliente());
    }

    @Test
    void debeBuscarPorTelefono() {
        Cliente cliente = crearClienteEjemplo();
        repositorio.anadirCliente(cliente);

        Optional<Cliente> resultado = repositorio.buscarTelefonoCliente(600123123);

        assertTrue(resultado.isPresent());
        assertEquals(cliente.getTelefonoCliente(), resultado.get().getTelefonoCliente());
    }

    @Test
    void debeRetornarOptionalVacioSiNoExiste() {
        Optional<Cliente> resultado = repositorio.buscarIdCliente(999999L);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void debeAsignarIdAlAnadirCliente() {
        Cliente cliente = crearClienteEjemplo();

        repositorio.anadirCliente(cliente);

        assertTrue(cliente.getIdCliente() > 0);
    }

    @Test
    void debeBuscarPorEmailIgnorandoMayusculas() {
        Cliente cliente = crearClienteEjemplo();
        repositorio.anadirCliente(cliente);

        Optional<Cliente> resultado = repositorio.buscarEmailCliente("CARLOS@EMAIL.COM");

        assertTrue(resultado.isPresent());
        assertEquals(cliente.getEmailCliente(), resultado.get().getEmailCliente());
    }

    @Test
    void obtenerClientes_debeRetornarTodosLosClientes() {
        Cliente cliente1 = crearClienteEjemplo();

        Cliente cliente2 = Cliente.builder()
                .nombreCliente("Ana")
                .apellidosCliente("Ruiz")
                .dniNifCliente("87654321B")
                .emailCliente("ana@email.com")
                .telefonoCliente(611111111)
                .fechaCreacionCliente(LocalDateTime.now())
                .build();

        repositorio.anadirCliente(cliente1);
        repositorio.anadirCliente(cliente2);

        assertEquals(2, repositorio.obtenerClientes().size());
    }
}
