package com.novabank.persistence.jdbc;

import com.novabank.config.DatabaseConnectionManager;
import com.novabank.domain.model.Cliente;
import com.novabank.domain.model.Cuenta;
import com.novabank.domain.model.Movimiento;
import com.novabank.domain.model.TipoMovimiento;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de integración para MovimientoRepositoryJdbc.
 *
 * Requiere PostgreSQL disponible y configuración JDBC operativa.
 */
class MovimientoRepositoryJdbcIT {

    private ClienteRepositoryJdbc clienteRepositoryJdbc;
    private CuentaRepositoryJdbc cuentaRepositoryJdbc;
    private MovimientoRepositoryJdbc movimientoRepositoryJdbc;

    @BeforeEach
    void setUp() throws SQLException {
        limpiarBaseDeDatos();

        clienteRepositoryJdbc = new ClienteRepositoryJdbc();
        cuentaRepositoryJdbc = new CuentaRepositoryJdbc();
        movimientoRepositoryJdbc = new MovimientoRepositoryJdbc();
    }

    @AfterEach
    void tearDown() throws SQLException {
        limpiarBaseDeDatos();
    }

    @Test
    void guardarMovimiento_yObtenerMovimientosCuenta_debePersistirYRecuperarMovimiento() {
        Cuenta cuenta = crearCuentaPersistida();

        Movimiento movimiento = Movimiento.builder()
                .cuentaAsignada(cuenta)
                .tipoMov(TipoMovimiento.DEPOSITO)
                .cantidadMovimiento(BigDecimal.valueOf(45.50))
                .fechaCreacionMov(LocalDateTime.now())
                .build();

        movimientoRepositoryJdbc.guardarMovimiento(movimiento);

        List<Movimiento> movimientos =
                movimientoRepositoryJdbc.obtenerMovimientosCuenta(cuenta.getNumeroCuenta());

        assertNotNull(movimientos);
        assertFalse(movimientos.isEmpty());
        assertNotNull(movimiento.getIdMovimiento());
        assertTrue(movimiento.getIdMovimiento() > 0);

        Movimiento recuperado = movimientos.get(0);
        assertEquals(TipoMovimiento.DEPOSITO, recuperado.getTipoMov());
        assertEquals(0, BigDecimal.valueOf(45.50).compareTo(recuperado.getCantidadMovimiento()));
        assertEquals(cuenta.getNumeroCuenta(), recuperado.getCuentaAsignada().getNumeroCuenta());
    }

    @Test
    void obtenerMovimientosFecha_debeFiltrarPorRango() {
        Cuenta cuenta = crearCuentaPersistida();

        Movimiento movimiento = Movimiento.builder()
                .cuentaAsignada(cuenta)
                .tipoMov(TipoMovimiento.RETIRO)
                .cantidadMovimiento(BigDecimal.valueOf(10))
                .fechaCreacionMov(LocalDateTime.now())
                .build();

        movimientoRepositoryJdbc.guardarMovimiento(movimiento);

        List<Movimiento> movimientos = movimientoRepositoryJdbc.obtenerMovimientosFecha(
                cuenta.getNumeroCuenta(),
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1)
        );

        assertNotNull(movimientos);
        assertFalse(movimientos.isEmpty());
        assertTrue(
                movimientos.stream().anyMatch(m ->
                        m.getTipoMov() == TipoMovimiento.RETIRO
                                && BigDecimal.valueOf(10).compareTo(m.getCantidadMovimiento()) == 0
                )
        );
    }

    @Test
    void obtenerMovimientosCuenta_sinResultados_debeRetornarListaVacia() {
        List<Movimiento> movimientos =
                movimientoRepositoryJdbc.obtenerMovimientosCuenta("ES00000000000000000000");

        assertNotNull(movimientos);
        assertTrue(movimientos.isEmpty());
    }

    @Test
    void guardarMovimiento_debeAsignarIdGeneradoPorPostgreSQL() {
        Cuenta cuenta = crearCuentaPersistida();

        Movimiento movimiento = Movimiento.builder()
                .cuentaAsignada(cuenta)
                .tipoMov(TipoMovimiento.DEPOSITO)
                .cantidadMovimiento(BigDecimal.valueOf(20))
                .fechaCreacionMov(LocalDateTime.now())
                .build();

        movimientoRepositoryJdbc.guardarMovimiento(movimiento);

        assertNotNull(movimiento.getIdMovimiento());
        assertTrue(movimiento.getIdMovimiento() > 0);
    }

    @Test
    void obtenerMovimientosCuenta_debeRetornarSoloMovimientosDeLaCuentaIndicada() {
        Cuenta cuenta1 = crearCuentaPersistida();
        Cuenta cuenta2 = crearCuentaPersistida();

        Movimiento movimiento1 = Movimiento.builder()
                .cuentaAsignada(cuenta1)
                .tipoMov(TipoMovimiento.DEPOSITO)
                .cantidadMovimiento(BigDecimal.valueOf(10))
                .fechaCreacionMov(LocalDateTime.now())
                .build();

        Movimiento movimiento2 = Movimiento.builder()
                .cuentaAsignada(cuenta2)
                .tipoMov(TipoMovimiento.RETIRO)
                .cantidadMovimiento(BigDecimal.valueOf(5))
                .fechaCreacionMov(LocalDateTime.now())
                .build();

        movimientoRepositoryJdbc.guardarMovimiento(movimiento1);
        movimientoRepositoryJdbc.guardarMovimiento(movimiento2);

        List<Movimiento> movimientos = movimientoRepositoryJdbc.obtenerMovimientosCuenta(cuenta1.getNumeroCuenta());

        assertEquals(1, movimientos.size());
        assertEquals(cuenta1.getNumeroCuenta(), movimientos.get(0).getCuentaAsignada().getNumeroCuenta());
    }

    @Test
    void obtenerMovimientosFecha_fueraDeRango_debeRetornarListaVacia() {
        Cuenta cuenta = crearCuentaPersistida();

        Movimiento movimiento = Movimiento.builder()
                .cuentaAsignada(cuenta)
                .tipoMov(TipoMovimiento.DEPOSITO)
                .cantidadMovimiento(BigDecimal.valueOf(10))
                .fechaCreacionMov(LocalDateTime.now())
                .build();

        movimientoRepositoryJdbc.guardarMovimiento(movimiento);

        List<Movimiento> movimientos = movimientoRepositoryJdbc.obtenerMovimientosFecha(
                cuenta.getNumeroCuenta(),
                LocalDate.now().minusDays(10),
                LocalDate.now().minusDays(5)
        );

        assertTrue(movimientos.isEmpty());
    }

    private void limpiarBaseDeDatos() throws SQLException {
        try (Connection connection = DatabaseConnectionManager.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("TRUNCATE TABLE movimientos, cuentas, clientes RESTART IDENTITY CASCADE");
        }
    }

    private Cuenta crearCuentaPersistida() {
        Cliente cliente = crearClientePersistido();

        Cuenta cuenta = Cuenta.builder()
                .dueñoCuenta(cliente)
                .numeroCuenta(generarNumeroCuentaUnico())
                .saldoCuenta(BigDecimal.valueOf(100))
                .fechaCreacionCuenta(LocalDateTime.now())
                .build();

        cuentaRepositoryJdbc.guardarCuenta(cuenta);
        return cuenta;
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