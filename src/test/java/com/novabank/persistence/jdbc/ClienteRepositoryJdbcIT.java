package com.novabank.persistence.jdbc;

import com.novabank.config.DatabaseConnectionManager;
import com.novabank.domain.model.Cliente;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
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
    void setUp() throws SQLException {
        limpiarBaseDeDatos();
        clienteRepositoryJdbc = new ClienteRepositoryJdbc();
    }

    @AfterEach
    void tearDown() throws SQLException {
        limpiarBaseDeDatos();
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

    private void limpiarBaseDeDatos() throws SQLException {
        try (Connection connection = DatabaseConnectionManager.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("TRUNCATE TABLE movimientos, cuentas, clientes RESTART IDENTITY CASCADE");
        }
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
}