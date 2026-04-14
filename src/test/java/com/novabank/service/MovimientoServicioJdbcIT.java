package com.novabank.service;

import com.novabank.config.DatabaseConnectionManager;
import com.novabank.domain.model.Cliente;
import com.novabank.domain.model.Cuenta;
import com.novabank.domain.model.Movimiento;
import com.novabank.domain.model.TipoMovimiento;
import com.novabank.exception.InsufficientBalanceException;
import com.novabank.exception.ResourceNotFoundException;
import com.novabank.exception.ValidationException;
import com.novabank.persistence.jdbc.ClienteRepositoryJdbc;
import com.novabank.persistence.jdbc.CuentaRepositoryJdbc;
import com.novabank.persistence.jdbc.MovimientoRepositoryJdbc;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test de integración para las operaciones JDBC transaccionales.
 *
 * Requiere PostgreSQL disponible y configuración JDBC operativa.
 */
class MovimientoServicioJdbcIT {

    private ClienteRepositoryJdbc clienteRepositoryJdbc;
    private CuentaRepositoryJdbc cuentaRepositoryJdbc;
    private MovimientoRepositoryJdbc movimientoRepositoryJdbc;
    private MovimientoServicio movimientoServicio;

    @BeforeEach
    void setUp() throws SQLException {
        limpiarBaseDeDatos();

        clienteRepositoryJdbc = new ClienteRepositoryJdbc();
        cuentaRepositoryJdbc = new CuentaRepositoryJdbc();
        movimientoRepositoryJdbc = new MovimientoRepositoryJdbc();
        movimientoServicio = new MovimientoServicio(cuentaRepositoryJdbc, movimientoRepositoryJdbc);
    }

    @AfterEach
    void tearDown() throws SQLException {
        limpiarBaseDeDatos();
    }

    @Test
    void depositar_debePersistirSaldoYMovimiento() {
        Cuenta cuenta = crearCuentaPersistida(BigDecimal.valueOf(100));

        movimientoServicio.depositar(cuenta.getNumeroCuenta(), BigDecimal.valueOf(25));

        Cuenta recuperada = cuentaRepositoryJdbc.buscarNumeroCuenta(cuenta.getNumeroCuenta()).orElseThrow();
        List<Movimiento> movimientos = movimientoRepositoryJdbc.obtenerMovimientosCuenta(cuenta.getNumeroCuenta());

        assertEquals(0, BigDecimal.valueOf(125).compareTo(recuperada.getSaldoCuenta()));
        assertTrue(
                movimientos.stream().anyMatch(m ->
                        m.getTipoMov() == TipoMovimiento.DEPOSITO
                                && BigDecimal.valueOf(25).compareTo(m.getCantidadMovimiento()) == 0
                )
        );
    }

    @Test
    void retirar_debePersistirSaldoYMovimiento() {
        Cuenta cuenta = crearCuentaPersistida(BigDecimal.valueOf(100));

        movimientoServicio.retirar(cuenta.getNumeroCuenta(), BigDecimal.valueOf(40));

        Cuenta recuperada = cuentaRepositoryJdbc.buscarNumeroCuenta(cuenta.getNumeroCuenta()).orElseThrow();
        List<Movimiento> movimientos = movimientoRepositoryJdbc.obtenerMovimientosCuenta(cuenta.getNumeroCuenta());

        assertEquals(0, BigDecimal.valueOf(60).compareTo(recuperada.getSaldoCuenta()));
        assertTrue(
                movimientos.stream().anyMatch(m ->
                        m.getTipoMov() == TipoMovimiento.RETIRO
                                && BigDecimal.valueOf(40).compareTo(m.getCantidadMovimiento()) == 0
                )
        );
    }

    @Test
    void transferir_debePersistirSaldosYRegistrarDosMovimientos() {
        Cuenta origen = crearCuentaPersistida(BigDecimal.valueOf(100));
        Cuenta destino = crearCuentaPersistida(BigDecimal.valueOf(20));

        movimientoServicio.transferir(
                origen.getNumeroCuenta(),
                destino.getNumeroCuenta(),
                BigDecimal.valueOf(30)
        );

        Cuenta origenRecuperada = cuentaRepositoryJdbc.buscarNumeroCuenta(origen.getNumeroCuenta()).orElseThrow();
        Cuenta destinoRecuperada = cuentaRepositoryJdbc.buscarNumeroCuenta(destino.getNumeroCuenta()).orElseThrow();

        List<Movimiento> movimientosOrigen = movimientoRepositoryJdbc.obtenerMovimientosCuenta(origen.getNumeroCuenta());
        List<Movimiento> movimientosDestino = movimientoRepositoryJdbc.obtenerMovimientosCuenta(destino.getNumeroCuenta());

        assertEquals(0, BigDecimal.valueOf(70).compareTo(origenRecuperada.getSaldoCuenta()));
        assertEquals(0, BigDecimal.valueOf(50).compareTo(destinoRecuperada.getSaldoCuenta()));

        assertTrue(
                movimientosOrigen.stream().anyMatch(m ->
                        m.getTipoMov() == TipoMovimiento.TRANSFERENCIA_SALIENTE
                                && BigDecimal.valueOf(30).compareTo(m.getCantidadMovimiento()) == 0
                )
        );

        assertTrue(
                movimientosDestino.stream().anyMatch(m ->
                        m.getTipoMov() == TipoMovimiento.TRANSFERENCIA_ENTRANTE
                                && BigDecimal.valueOf(30).compareTo(m.getCantidadMovimiento()) == 0
                )
        );
    }

    @Test
    void transferir_conSaldoInsuficiente_noDebePersistirCambiosParciales() {
        Cuenta origen = crearCuentaPersistida(BigDecimal.valueOf(20));
        Cuenta destino = crearCuentaPersistida(BigDecimal.valueOf(50));

        assertThrows(
                InsufficientBalanceException.class,
                () -> movimientoServicio.transferir(
                        origen.getNumeroCuenta(),
                        destino.getNumeroCuenta(),
                        BigDecimal.valueOf(100)
                )
        );

        Cuenta origenRecuperada = cuentaRepositoryJdbc.buscarNumeroCuenta(origen.getNumeroCuenta()).orElseThrow();
        Cuenta destinoRecuperada = cuentaRepositoryJdbc.buscarNumeroCuenta(destino.getNumeroCuenta()).orElseThrow();

        List<Movimiento> movimientosOrigen = movimientoRepositoryJdbc.obtenerMovimientosCuenta(origen.getNumeroCuenta());
        List<Movimiento> movimientosDestino = movimientoRepositoryJdbc.obtenerMovimientosCuenta(destino.getNumeroCuenta());

        assertEquals(0, BigDecimal.valueOf(20).compareTo(origenRecuperada.getSaldoCuenta()));
        assertEquals(0, BigDecimal.valueOf(50).compareTo(destinoRecuperada.getSaldoCuenta()));

        assertTrue(
                movimientosOrigen.stream().noneMatch(m ->
                        m.getTipoMov() == TipoMovimiento.TRANSFERENCIA_SALIENTE
                                && BigDecimal.valueOf(100).compareTo(m.getCantidadMovimiento()) == 0
                )
        );

        assertTrue(
                movimientosDestino.stream().noneMatch(m ->
                        m.getTipoMov() == TipoMovimiento.TRANSFERENCIA_ENTRANTE
                                && BigDecimal.valueOf(100).compareTo(m.getCantidadMovimiento()) == 0
                )
        );
    }

    @Test
    void transferir_conCuentaDestinoInexistente_noDebeAlterarElSaldoOrigen() {
        Cuenta origen = crearCuentaPersistida(BigDecimal.valueOf(100));

        assertThrows(
                ResourceNotFoundException.class,
                () -> movimientoServicio.transferir(
                        origen.getNumeroCuenta(),
                        "ES91999999999999999999",
                        BigDecimal.valueOf(30)
                )
        );

        Cuenta origenRecuperada = cuentaRepositoryJdbc.buscarNumeroCuenta(origen.getNumeroCuenta()).orElseThrow();
        List<Movimiento> movimientosOrigen = movimientoRepositoryJdbc.obtenerMovimientosCuenta(origen.getNumeroCuenta());

        assertEquals(0, BigDecimal.valueOf(100).compareTo(origenRecuperada.getSaldoCuenta()));

        assertTrue(
                movimientosOrigen.stream().noneMatch(m ->
                        (m.getTipoMov() == TipoMovimiento.TRANSFERENCIA_SALIENTE
                                || m.getTipoMov() == TipoMovimiento.TRANSFERENCIA_ENTRANTE)
                                && BigDecimal.valueOf(30).compareTo(m.getCantidadMovimiento()) == 0
                )
        );
    }

    @Test
    void depositar_conCuentaInexistente_debeLanzarExcepcionSinPersistirMovimiento() {
        assertThrows(
                ResourceNotFoundException.class,
                () -> movimientoServicio.depositar("ES91999999999999999999", BigDecimal.valueOf(25))
        );
    }

    @Test
    void retirar_conSaldoInsuficiente_noDebePersistirCambiosParciales() {
        Cuenta cuenta = crearCuentaPersistida(BigDecimal.valueOf(20));

        assertThrows(
                InsufficientBalanceException.class,
                () -> movimientoServicio.retirar(cuenta.getNumeroCuenta(), BigDecimal.valueOf(100))
        );

        Cuenta recuperada = cuentaRepositoryJdbc.buscarNumeroCuenta(cuenta.getNumeroCuenta()).orElseThrow();
        List<Movimiento> movimientos = movimientoRepositoryJdbc.obtenerMovimientosCuenta(cuenta.getNumeroCuenta());

        assertEquals(0, BigDecimal.valueOf(20).compareTo(recuperada.getSaldoCuenta()));

        assertTrue(
                movimientos.stream().noneMatch(m ->
                        m.getTipoMov() == TipoMovimiento.RETIRO
                                && BigDecimal.valueOf(100).compareTo(m.getCantidadMovimiento()) == 0
                )
        );
    }

    @Test
    void transferir_conMismaCuenta_debeLanzarExcepcionSinPersistirNada() {
        Cuenta cuenta = crearCuentaPersistida(BigDecimal.valueOf(100));

        assertThrows(
                ValidationException.class,
                () -> movimientoServicio.transferir(
                        cuenta.getNumeroCuenta(),
                        cuenta.getNumeroCuenta(),
                        BigDecimal.valueOf(10)
                )
        );

        Cuenta recuperada = cuentaRepositoryJdbc.buscarNumeroCuenta(cuenta.getNumeroCuenta()).orElseThrow();
        List<Movimiento> movimientos = movimientoRepositoryJdbc.obtenerMovimientosCuenta(cuenta.getNumeroCuenta());

        assertEquals(0, BigDecimal.valueOf(100).compareTo(recuperada.getSaldoCuenta()));
        assertTrue(
                movimientos.stream().noneMatch(m ->
                        (m.getTipoMov() == TipoMovimiento.TRANSFERENCIA_SALIENTE
                                || m.getTipoMov() == TipoMovimiento.TRANSFERENCIA_ENTRANTE)
                                && BigDecimal.valueOf(10).compareTo(m.getCantidadMovimiento()) == 0
                )
        );
    }

    private void limpiarBaseDeDatos() throws SQLException {
        try (Connection connection = DatabaseConnectionManager.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("TRUNCATE TABLE movimientos, cuentas, clientes RESTART IDENTITY CASCADE");
        }
    }

    private Cuenta crearCuentaPersistida(BigDecimal saldoInicial) {
        Cliente cliente = crearClientePersistido();

        Cuenta cuenta = Cuenta.builder()
                .dueñoCuenta(cliente)
                .numeroCuenta(generarNumeroCuentaUnico())
                .saldoCuenta(saldoInicial)
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