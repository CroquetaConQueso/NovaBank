package com.novabank.persistence.jdbc;

import com.novabank.domain.model.Cliente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de integración para ClienteRepositoryJdbc.
 *
 * Requiere PostgreSQL disponible y variables de entorno NOVABANK_DB_* configuradas.
 */
class ClienteRepositoryJdbcIT {

    private ClienteRepositoryJdbc clienteRepositoryJdbc;

    @BeforeEach
    void setUp() {
        clienteRepositoryJdbc = new ClienteRepositoryJdbc();
    }

    @Test
    void anadirCliente_yBuscarPorId_debePersistirYRecuperarCliente() {
        Cliente cliente = crearClienteUnico();

        clienteRepositoryJdbc.anadirCliente(cliente);

        Optional<Cliente> recuperado = clienteRepositoryJdbc.buscarIdCliente(cliente.getIdCliente());

        assertTrue(recuperado.isPresent());
        assertTrue(cliente.getIdCliente() > 0);
        assertEquals(cliente.getDniNifCliente(), recuperado.get().getDniNifCliente());
    }

    @Test
    void buscarDniCliente_debeRetornarClientePersistido() {
        Cliente cliente = crearClienteUnico();
        clienteRepositoryJdbc.anadirCliente(cliente);

        Optional<Cliente> recuperado = clienteRepositoryJdbc.buscarDniCliente(cliente.getDniNifCliente());

        assertTrue(recuperado.isPresent());
        assertEquals(cliente.getEmailCliente(), recuperado.get().getEmailCliente());
    }

    @Test
    void buscarEmailCliente_debeRetornarClientePersistido() {
        Cliente cliente = crearClienteUnico();
        clienteRepositoryJdbc.anadirCliente(cliente);

        Optional<Cliente> recuperado = clienteRepositoryJdbc.buscarEmailCliente(cliente.getEmailCliente());

        assertTrue(recuperado.isPresent());
        assertEquals(cliente.getDniNifCliente(), recuperado.get().getDniNifCliente());
    }

    @Test
    void buscarTelefonoCliente_debeRetornarClientePersistido() {
        Cliente cliente = crearClienteUnico();
        clienteRepositoryJdbc.anadirCliente(cliente);

        Optional<Cliente> recuperado = clienteRepositoryJdbc.buscarTelefonoCliente(cliente.getTelefonoCliente());

        assertTrue(recuperado.isPresent());
        assertEquals(cliente.getEmailCliente(), recuperado.get().getEmailCliente());
    }

    @Test
    void obtenerClientes_debeIncluirClientePersistido() {
        Cliente cliente = crearClienteUnico();
        clienteRepositoryJdbc.anadirCliente(cliente);

        List<Cliente> clientes = clienteRepositoryJdbc.obtenerClientes();

        assertTrue(
                clientes.stream().anyMatch(c -> c.getEmailCliente().equals(cliente.getEmailCliente()))
        );
    }

    private Cliente crearClienteUnico() {
        String sufijo = String.valueOf(System.nanoTime());
        String ochoDigitos = String.format("%08d", Math.abs((int) (System.nanoTime() % 100_000_000L)));
        int telefono = Integer.parseInt("6" + ochoDigitos.substring(1));

        return Cliente.builder()
                .nombreCliente("Carlos")
                .apellidosCliente("Torres")
                .dniNifCliente(ochoDigitos + "Z")
                .emailCliente("carlos" + sufijo + "@example.com")
                .telefonoCliente(telefono)
                .fechaCreacionCliente(LocalDateTime.now())
                .build();
    }

    @Test
    void buscarIdCliente_inexistente_debeRetornarOptionalVacio() {
        Optional<Cliente> resultado = clienteRepositoryJdbc.buscarIdCliente(Long.MAX_VALUE);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void buscarEmailCliente_debeIgnorarMayusculasYMinusculas() {
        Cliente cliente = crearClienteUnico();
        clienteRepositoryJdbc.anadirCliente(cliente);

        Optional<Cliente> recuperado =
                clienteRepositoryJdbc.buscarEmailCliente(cliente.getEmailCliente().toUpperCase());

        assertTrue(recuperado.isPresent());
        assertEquals(cliente.getDniNifCliente(), recuperado.get().getDniNifCliente());
    }

    @Test
    void buscarTelefonoCliente_inexistente_debeRetornarOptionalVacio() {
        Optional<Cliente> recuperado = clienteRepositoryJdbc.buscarTelefonoCliente(699999999);

        assertTrue(recuperado.isEmpty());
    }
}