package com.novabank.persistence.jdbc;

import com.novabank.config.DatabaseConnectionManager;
import com.novabank.domain.model.Cliente;
import com.novabank.domain.model.Cuenta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de integración para CuentaRepositoryJdbc.
 *
 * Requiere PostgreSQL disponible y variables de entorno NOVABANK_DB_* configuradas.
 */
class CuentaRepositoryJdbcIT {

    private ClienteRepositoryJdbc clienteRepositoryJdbc;
    private CuentaRepositoryJdbc cuentaRepositoryJdbc;

    @BeforeEach
    void setUp() throws SQLException {
        limpiarBaseDeDatos();

        clienteRepositoryJdbc = new ClienteRepositoryJdbc();
        cuentaRepositoryJdbc = new CuentaRepositoryJdbc();
    }

    @AfterEach
    void tearDown() throws SQLException {
        limpiarBaseDeDatos();
    }

    @Test
    void guardarCuenta_yBuscarNumeroCuenta_debePersistirYRecuperarCuenta() {
        Cliente cliente = crearClientePersistido();

        Cuenta cuenta = Cuenta.builder()
                .dueñoCuenta(cliente)
                .numeroCuenta(generarNumeroCuentaUnico())
                .saldoCuenta(BigDecimal.valueOf(100))
                .fechaCreacionCuenta(LocalDateTime.now())
                .build();

        cuentaRepositoryJdbc.guardarCuenta(cuenta);

        Optional<Cuenta> recuperada = cuentaRepositoryJdbc.buscarNumeroCuenta(cuenta.getNumeroCuenta());

        assertTrue(recuperada.isPresent());
        assertTrue(cuenta.getIdCuenta() > 0);
        assertEquals(cuenta.getNumeroCuenta(), recuperada.get().getNumeroCuenta());
        assertEquals(0, BigDecimal.valueOf(100).compareTo(recuperada.get().getSaldoCuenta()));
    }

    @Test
    void listarCuentasCliente_debeRetornarCuentasDelCliente() {
        Cliente cliente = crearClientePersistido();

        Cuenta cuenta1 = Cuenta.builder()
                .dueñoCuenta(cliente)
                .numeroCuenta(generarNumeroCuentaUnico())
                .saldoCuenta(BigDecimal.valueOf(100))
                .fechaCreacionCuenta(LocalDateTime.now())
                .build();

        Cuenta cuenta2 = Cuenta.builder()
                .dueñoCuenta(cliente)
                .numeroCuenta(generarNumeroCuentaUnico())
                .saldoCuenta(BigDecimal.valueOf(200))
                .fechaCreacionCuenta(LocalDateTime.now())
                .build();

        cuentaRepositoryJdbc.guardarCuenta(cuenta1);
        cuentaRepositoryJdbc.guardarCuenta(cuenta2);

        List<Cuenta> cuentas = cuentaRepositoryJdbc.listarCuentasCliente(cliente.getIdCliente());

        assertEquals(2, cuentas.size());
        assertTrue(cuentas.stream().anyMatch(c -> c.getNumeroCuenta().equals(cuenta1.getNumeroCuenta())));
        assertTrue(cuentas.stream().anyMatch(c -> c.getNumeroCuenta().equals(cuenta2.getNumeroCuenta())));
    }

    @Test
    void guardarCuenta_conMismoNumero_debeActualizarSinDuplicar() {
        Cliente cliente = crearClientePersistido();
        String numeroCuenta = generarNumeroCuentaUnico();

        Cuenta cuentaOriginal = Cuenta.builder()
                .dueñoCuenta(cliente)
                .numeroCuenta(numeroCuenta)
                .saldoCuenta(BigDecimal.valueOf(100))
                .fechaCreacionCuenta(LocalDateTime.now())
                .build();

        cuentaRepositoryJdbc.guardarCuenta(cuentaOriginal);

        Cuenta cuentaActualizada = Cuenta.builder()
                .dueñoCuenta(cliente)
                .numeroCuenta(numeroCuenta)
                .saldoCuenta(BigDecimal.valueOf(250))
                .fechaCreacionCuenta(LocalDateTime.now())
                .build();

        cuentaRepositoryJdbc.guardarCuenta(cuentaActualizada);

        Cuenta recuperada = cuentaRepositoryJdbc.buscarNumeroCuenta(numeroCuenta).orElseThrow();
        List<Cuenta> cuentasCliente = cuentaRepositoryJdbc.listarCuentasCliente(cliente.getIdCliente());

        long coincidencias = cuentasCliente.stream()
                .filter(c -> c.getNumeroCuenta().equals(numeroCuenta))
                .count();

        assertEquals(1L, coincidencias);
        assertEquals(0, BigDecimal.valueOf(250).compareTo(recuperada.getSaldoCuenta()));
    }

    @Test
    void buscarNumeroCuenta_inexistente_debeRetornarOptionalVacio() {
        Optional<Cuenta> resultado = cuentaRepositoryJdbc.buscarNumeroCuenta("ES00000000000000000000");

        assertTrue(resultado.isEmpty());
    }

    @Test
    void listarCuentasCliente_conClienteSinCuentas_debeRetornarListaVacia() {
        Cliente cliente = crearClientePersistido();

        List<Cuenta> cuentas = cuentaRepositoryJdbc.listarCuentasCliente(cliente.getIdCliente());

        assertNotNull(cuentas);
        assertTrue(cuentas.isEmpty());
    }

    @Test
    void actualizarSaldo_debePersistirElNuevoSaldo() throws Exception {
        Cliente cliente = crearClientePersistido();

        Cuenta cuenta = Cuenta.builder()
                .dueñoCuenta(cliente)
                .numeroCuenta(generarNumeroCuentaUnico())
                .saldoCuenta(BigDecimal.valueOf(100))
                .fechaCreacionCuenta(LocalDateTime.now())
                .build();

        cuentaRepositoryJdbc.guardarCuenta(cuenta);

        try (Connection connection = DatabaseConnectionManager.getConnection()) {
            connection.setAutoCommit(false);

            cuentaRepositoryJdbc.actualizarSaldo(connection, cuenta.getNumeroCuenta(), BigDecimal.valueOf(350));

            connection.commit();
        }

        Cuenta recuperada = cuentaRepositoryJdbc.buscarNumeroCuenta(cuenta.getNumeroCuenta()).orElseThrow();

        assertEquals(0, BigDecimal.valueOf(350).compareTo(recuperada.getSaldoCuenta()));
    }

    @Test
    void buscarNumeroCuenta_conConnectionDentroDeTransaccion_debeRetornarCuenta() throws Exception {
        Cliente cliente = crearClientePersistido();

        Cuenta cuenta = Cuenta.builder()
                .dueñoCuenta(cliente)
                .numeroCuenta(generarNumeroCuentaUnico())
                .saldoCuenta(BigDecimal.valueOf(100))
                .fechaCreacionCuenta(LocalDateTime.now())
                .build();

        cuentaRepositoryJdbc.guardarCuenta(cuenta);

        try (Connection connection = DatabaseConnectionManager.getConnection()) {
            connection.setAutoCommit(false);

            Optional<Cuenta> recuperada =
                    cuentaRepositoryJdbc.buscarNumeroCuenta(cuenta.getNumeroCuenta(), connection);

            assertTrue(recuperada.isPresent());
            assertEquals(cuenta.getNumeroCuenta(), recuperada.get().getNumeroCuenta());

            connection.rollback();
        }
    }

    @Test
    void obtenerUltimoIdCuenta_sinCuentas_debeRetornarCero() {
        long ultimoId = cuentaRepositoryJdbc.obtenerUltimoIdCuenta();

        assertEquals(0L, ultimoId);
    }

    @Test
    void obtenerUltimoIdCuenta_conCuentasPersistidas_debeRetornarElMayorId() {
        Cliente cliente = crearClientePersistido();

        Cuenta cuenta1 = Cuenta.builder()
                .dueñoCuenta(cliente)
                .numeroCuenta(generarNumeroCuentaUnico())
                .saldoCuenta(BigDecimal.valueOf(100))
                .fechaCreacionCuenta(LocalDateTime.now())
                .build();

        Cuenta cuenta2 = Cuenta.builder()
                .dueñoCuenta(cliente)
                .numeroCuenta(generarNumeroCuentaUnico())
                .saldoCuenta(BigDecimal.valueOf(200))
                .fechaCreacionCuenta(LocalDateTime.now())
                .build();

        cuentaRepositoryJdbc.guardarCuenta(cuenta1);
        cuentaRepositoryJdbc.guardarCuenta(cuenta2);

        long ultimoId = cuentaRepositoryJdbc.obtenerUltimoIdCuenta();

        assertEquals(cuenta2.getIdCuenta(), ultimoId);
    }

    private void limpiarBaseDeDatos() throws SQLException {
        try (Connection connection = DatabaseConnectionManager.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("TRUNCATE TABLE movimientos, cuentas, clientes RESTART IDENTITY CASCADE");
        }
    }

    private Cliente crearClientePersistido() {
        String sufijo = String.valueOf(System.nanoTime());
        String ochoDigitos = String.format("%08d", Math.abs((int) (System.nanoTime() % 100_000_000L)));
        int telefono = Integer.parseInt("6" + ochoDigitos.substring(1));

        Cliente cliente = Cliente.builder()
                .nombreCliente("Carlos")
                .apellidosCliente("Torres")
                .dniNifCliente(ochoDigitos + "Z")
                .emailCliente("carlos" + sufijo + "@example.com")
                .telefonoCliente(telefono)
                .fechaCreacionCliente(LocalDateTime.now())
                .build();

        clienteRepositoryJdbc.anadirCliente(cliente);
        return cliente;
    }

    private String generarNumeroCuentaUnico() {
        long sufijo = Math.abs(System.nanoTime() % 1_000_000_000_000_000_000L);
        return "ES91" + String.format("%018d", sufijo);
    }
}