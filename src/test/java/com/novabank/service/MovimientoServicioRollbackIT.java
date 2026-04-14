package com.novabank.service;

import com.novabank.config.DatabaseConnectionManager;
import com.novabank.domain.model.Cliente;
import com.novabank.domain.model.Cuenta;
import com.novabank.domain.model.Movimiento;
import com.novabank.exception.NovaBankException;
import com.novabank.persistence.jdbc.ClienteRepositoryJdbc;
import com.novabank.persistence.jdbc.CuentaRepositoryJdbc;
import com.novabank.persistence.jdbc.MovimientoRepositoryJdbc;
import com.novabank.persistence.repository.ClienteRepository;
import com.novabank.persistence.repository.CuentaRepository;
import com.novabank.persistence.repository.MovimientoRepository;
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

class MovimientoServicioRollbackIT {

    private final ClienteRepository repoClienteReal = new ClienteRepositoryJdbc();
    private final CuentaRepository repoCuentaReal = new CuentaRepositoryJdbc();
    private final MovimientoRepository repoMovimientoReal = new MovimientoRepositoryJdbc();

    @BeforeEach
    void limpiarBaseDeDatosAntes() throws SQLException {
        limpiarBaseDeDatos();
    }

    @AfterEach
    void limpiarBaseDeDatosDespues() throws SQLException {
        limpiarBaseDeDatos();
    }

    @Test
    void transferir_siFallaElSegundoMovimiento_debeHacerRollbackReal() {
        Cuenta cuentaOrigen = crearCuentaPersistida(BigDecimal.valueOf(500));
        Cuenta cuentaDestino = crearCuentaPersistida(BigDecimal.valueOf(100));

        MovimientoRepository repoMovimientoQueFalla =
                new MovimientoRepositoryFalloEnSegundoGuardado(repoMovimientoReal);

        MovimientoServicio servicio = new MovimientoServicio(repoCuentaReal, repoMovimientoQueFalla);

        assertThrows(
                NovaBankException.class,
                () -> servicio.transferir(
                        cuentaOrigen.getNumeroCuenta(),
                        cuentaDestino.getNumeroCuenta(),
                        BigDecimal.valueOf(50)
                )
        );

        Cuenta origenRecargada = repoCuentaReal.buscarNumeroCuenta(cuentaOrigen.getNumeroCuenta()).orElseThrow();
        Cuenta destinoRecargada = repoCuentaReal.buscarNumeroCuenta(cuentaDestino.getNumeroCuenta()).orElseThrow();

        assertEquals(0, BigDecimal.valueOf(500).compareTo(origenRecargada.getSaldoCuenta()));
        assertEquals(0, BigDecimal.valueOf(100).compareTo(destinoRecargada.getSaldoCuenta()));

        List<Movimiento> movimientosOrigen =
                repoMovimientoReal.obtenerMovimientosCuenta(cuentaOrigen.getNumeroCuenta());
        List<Movimiento> movimientosDestino =
                repoMovimientoReal.obtenerMovimientosCuenta(cuentaDestino.getNumeroCuenta());

        assertEquals(0, movimientosOrigen.size());
        assertEquals(0, movimientosDestino.size());
    }

    @Test
    void transferir_siFallaLaSegundaActualizacionDeSaldo_debeHacerRollbackReal() {
        Cuenta cuentaOrigen = crearCuentaPersistida(BigDecimal.valueOf(800));
        Cuenta cuentaDestino = crearCuentaPersistida(BigDecimal.valueOf(120));

        CuentaRepository repoCuentaQueFalla =
                new CuentaRepositoryFalloEnSegundaActualizacion(repoCuentaReal);

        MovimientoServicio servicio = new MovimientoServicio(repoCuentaQueFalla, repoMovimientoReal);

        assertThrows(
                NovaBankException.class,
                () -> servicio.transferir(
                        cuentaOrigen.getNumeroCuenta(),
                        cuentaDestino.getNumeroCuenta(),
                        BigDecimal.valueOf(75)
                )
        );

        Cuenta origenRecargada = repoCuentaReal.buscarNumeroCuenta(cuentaOrigen.getNumeroCuenta()).orElseThrow();
        Cuenta destinoRecargada = repoCuentaReal.buscarNumeroCuenta(cuentaDestino.getNumeroCuenta()).orElseThrow();

        assertEquals(0, BigDecimal.valueOf(800).compareTo(origenRecargada.getSaldoCuenta()));
        assertEquals(0, BigDecimal.valueOf(120).compareTo(destinoRecargada.getSaldoCuenta()));

        List<Movimiento> movimientosOrigen =
                repoMovimientoReal.obtenerMovimientosCuenta(cuentaOrigen.getNumeroCuenta());
        List<Movimiento> movimientosDestino =
                repoMovimientoReal.obtenerMovimientosCuenta(cuentaDestino.getNumeroCuenta());

        assertEquals(0, movimientosOrigen.size());
        assertEquals(0, movimientosDestino.size());
    }

    private void limpiarBaseDeDatos() throws SQLException {
        try (Connection connection = DatabaseConnectionManager.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("TRUNCATE TABLE movimientos, cuentas, clientes RESTART IDENTITY CASCADE");
        }
    }

    private Cuenta crearCuentaPersistida(BigDecimal saldoInicial) {
        Cliente cliente = crearClientePersistido();
        String numeroCuenta = generarNumeroCuentaUnico();

        Cuenta cuenta = Cuenta.builder()
                .dueñoCuenta(cliente)
                .numeroCuenta(numeroCuenta)
                .saldoCuenta(saldoInicial)
                .fechaCreacionCuenta(LocalDateTime.now())
                .build();

        repoCuentaReal.guardarCuenta(cuenta);
        return cuenta;
    }

    private Cliente crearClientePersistido() {
        long sufijo = System.nanoTime();

        Cliente cliente = Cliente.builder()
                .nombreCliente("Carlos")
                .apellidosCliente("Torres")
                .dniNifCliente(generarDniValido(sufijo))
                .emailCliente("carlos" + sufijo + "@test.com")
                .telefonoCliente(generarTelefonoValido(sufijo))
                .fechaCreacionCliente(LocalDateTime.now())
                .build();

        repoClienteReal.anadirCliente(cliente);
        return cliente;
    }

    private String generarNumeroCuentaUnico() {
        long sufijo = Math.abs(System.nanoTime() % 1_000_000_000_000L);
        return "ES91210000" + String.format("%012d", sufijo);
    }

    private String generarDniValido(long base) {
        String numeros = String.format("%08d", Math.abs(base % 100_000_000L));
        return numeros + "Z";
    }

    private int generarTelefonoValido(long base) {
        return Integer.parseInt("6" + String.format("%08d", Math.abs(base % 100_000_000L)));
    }
}