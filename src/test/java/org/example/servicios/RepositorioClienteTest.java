package org.example.repositorio;

import org.example.modelos.Cliente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class RepositorioClienteTest {

    private RepositorioCliente repositorio;

    @BeforeEach
    void setUp() {
        repositorio = new RepositorioCliente();
    }

    private Cliente crearClienteEjemplo() {
        return new Cliente(
                "Carlos",
                "Torres",
                "12345678A",
                "carlos@email.com",
                600123123,
                LocalDateTime.now()
        );
    }

    @Test
    void debeAnadirClienteCorrectamente() {
        Cliente cliente = crearClienteEjemplo();

        repositorio.anadirCliente(cliente);

        Cliente resultado = repositorio.buscarIdCliente(cliente.getIdCliente());

        assertNotNull(resultado);
        assertEquals(cliente.getDniNifCliente(), resultado.getDniNifCliente());
    }

    @Test
    void noDebeSobrescribirClienteConMismoId() {
        Cliente cliente1 = crearClienteEjemplo();
        repositorio.anadirCliente(cliente1);

        // Intentamos añadir el mismo objeto otra vez
        repositorio.anadirCliente(cliente1);

        Cliente resultado = repositorio.buscarIdCliente(cliente1.getIdCliente());

        assertNotNull(resultado);
        assertEquals(cliente1.getEmailCliente(), resultado.getEmailCliente());
    }

    @Test
    void debeBuscarPorDni() {
        Cliente cliente = crearClienteEjemplo();
        repositorio.anadirCliente(cliente);

        Cliente resultado = repositorio.buscarDniCliente("12345678A");

        assertNotNull(resultado);
        assertEquals(cliente.getIdCliente(), resultado.getIdCliente());
    }

    @Test
    void debeBuscarPorEmail() {
        Cliente cliente = crearClienteEjemplo();
        repositorio.anadirCliente(cliente);

        Cliente resultado = repositorio.buscarEmailCliente("carlos@email.com");

        assertNotNull(resultado);
    }

    @Test
    void debeBuscarPorTelefono() {
        Cliente cliente = crearClienteEjemplo();
        repositorio.anadirCliente(cliente);

        Cliente resultado = repositorio.buscarTelefonoCliente(600123123);

        assertNotNull(resultado);
    }

    @Test
    void debeRetornarNullSiNoExiste() {
        Cliente resultado = repositorio.buscarIdCliente(999999L);

        assertNull(resultado);
    }
}
