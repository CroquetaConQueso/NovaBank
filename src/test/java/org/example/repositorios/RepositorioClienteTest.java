package org.example.repositorios;

import org.example.modelos.Cliente;
import org.example.repositorio.RepositorioCliente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para la clase RepositorioCliente.
 *
 * Verifica el correcto almacenamiento y recuperación
 * de clientes en memoria.
 */
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

    /**
     * Verifica que un cliente se añade correctamente
     * al repositorio y puede recuperarse por su identificador.
     */
    @Test
    void debeAnadirClienteCorrectamente() {
        Cliente cliente = crearClienteEjemplo();

        repositorio.anadirCliente(cliente);

        Cliente resultado = repositorio.buscarIdCliente(cliente.getIdCliente());

        assertNotNull(resultado);
        assertEquals(cliente.getDniNifCliente(), resultado.getDniNifCliente());
    }

    /**
     * Verifica que no se sobrescribe un cliente
     * cuando se intenta añadir nuevamente
     * con el mismo identificador.
     */
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

    /**
     * Verifica que es posible recuperar un cliente
     * mediante su DNI/NIF.
     */
    @Test
    void debeBuscarPorDni() {
        Cliente cliente = crearClienteEjemplo();
        repositorio.anadirCliente(cliente);

        Cliente resultado = repositorio.buscarDniCliente("12345678A");

        assertNotNull(resultado);
        assertEquals(cliente.getIdCliente(), resultado.getIdCliente());
    }

    /**
     * Verifica que es posible recuperar un cliente
     * mediante su dirección de correo electrónico.
     */
    @Test
    void debeBuscarPorEmail() {
        Cliente cliente = crearClienteEjemplo();
        repositorio.anadirCliente(cliente);

        Cliente resultado = repositorio.buscarEmailCliente("carlos@email.com");

        assertNotNull(resultado);
    }

    /**
     * Verifica que es posible recuperar un cliente
     * mediante su número de teléfono.
     */
    @Test
    void debeBuscarPorTelefono() {
        Cliente cliente = crearClienteEjemplo();
        repositorio.anadirCliente(cliente);

        Cliente resultado = repositorio.buscarTelefonoCliente(600123123);

        assertNotNull(resultado);
    }

    /**
     * Verifica que se devuelve null cuando
     * no existe ningún cliente con el ID indicado.
     */
    @Test
    void debeRetornarNullSiNoExiste() {
        Cliente resultado = repositorio.buscarIdCliente(999999L);

        assertNull(resultado);
    }
}
