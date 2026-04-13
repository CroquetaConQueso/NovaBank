package com.novabank.persistence.jdbc;

import com.novabank.domain.model.Cliente;
import com.novabank.domain.model.Cuenta;
import com.novabank.domain.model.Movimiento;
import com.novabank.domain.model.TipoMovimiento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de integración para MovimientoRepositoryJdbc.
 *
 * Requiere PostgreSQL disponible y variables de entorno NOVABANK_DB_* configuradas.
 */
class MovimientoRepositoryJdbcIT {

    private ClienteRepositoryJdbc clienteRepositoryJdbc;
    private CuentaRepositoryJdbc cuentaRepositoryJdbc;
    private MovimientoRepositoryJdbc movimientoRepositoryJdbc;

    @BeforeEach
    void setUp() {
        clienteRepositoryJdbc = new ClienteRepositoryJdbc();
        cuentaRepositoryJdbc = new CuentaRepositoryJdbc();
        movimientoRepositoryJdbc = new MovimientoRepositoryJdbc();
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
        String sufijo = String.valueOf(System.currentTimeMillis());
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