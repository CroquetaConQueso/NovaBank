package com.novabank.domain.factory;

import com.novabank.domain.model.Cliente;
import com.novabank.domain.model.Cuenta;
import com.novabank.domain.model.Movimiento;
import com.novabank.domain.model.TipoMovimiento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test unitario para MovimientoFactory.
 */
class MovimientoFactoryTest {

    @Test
    void crearDeposito_debeConstruirMovimientoValido() {
        Cuenta cuenta = crearCuenta();

        Movimiento movimiento = MovimientoFactory.crearDeposito(cuenta, BigDecimal.valueOf(25));

        assertNotNull(movimiento);
        assertEquals(cuenta, movimiento.getCuentaAsignada());
        assertEquals(TipoMovimiento.DEPOSITO, movimiento.getTipoMov());
        assertEquals(BigDecimal.valueOf(25), movimiento.getCantidadMovimiento());
        assertNotNull(movimiento.getFechaCreacionMov());
    }

    @Test
    void crearRetiro_debeConstruirMovimientoValido() {
        Cuenta cuenta = crearCuenta();

        Movimiento movimiento = MovimientoFactory.crearRetiro(cuenta, BigDecimal.valueOf(10));

        assertNotNull(movimiento);
        assertEquals(cuenta, movimiento.getCuentaAsignada());
        assertEquals(TipoMovimiento.RETIRO, movimiento.getTipoMov());
        assertEquals(BigDecimal.valueOf(10), movimiento.getCantidadMovimiento());
        assertNotNull(movimiento.getFechaCreacionMov());
    }

    @Test
    void crearTransferenciaSaliente_debeConstruirMovimientoValido() {
        Cuenta cuenta = crearCuenta();

        Movimiento movimiento = MovimientoFactory.crearTransferenciaSaliente(cuenta, BigDecimal.valueOf(40));

        assertNotNull(movimiento);
        assertEquals(cuenta, movimiento.getCuentaAsignada());
        assertEquals(TipoMovimiento.TRANSFERENCIA_SALIENTE, movimiento.getTipoMov());
        assertEquals(BigDecimal.valueOf(40), movimiento.getCantidadMovimiento());
        assertNotNull(movimiento.getFechaCreacionMov());
    }

    @Test
    void crearTransferenciaEntrante_debeConstruirMovimientoValido() {
        Cuenta cuenta = crearCuenta();

        Movimiento movimiento = MovimientoFactory.crearTransferenciaEntrante(cuenta, BigDecimal.valueOf(40));

        assertNotNull(movimiento);
        assertEquals(cuenta, movimiento.getCuentaAsignada());
        assertEquals(TipoMovimiento.TRANSFERENCIA_ENTRANTE, movimiento.getTipoMov());
        assertEquals(BigDecimal.valueOf(40), movimiento.getCantidadMovimiento());
        assertNotNull(movimiento.getFechaCreacionMov());
    }

    private Cuenta crearCuenta() {
        Cliente cliente = Cliente.builder()
                .nombreCliente("Carlos")
                .apellidosCliente("Torres")
                .dniNifCliente("12345678Z")
                .emailCliente("carlos@example.com")
                .telefonoCliente(612345678)
                .fechaCreacionCliente(LocalDateTime.now())
                .build();

        return Cuenta.builder()
                .dueñoCuenta(cliente)
                .numeroCuenta("ES12345678901234567890")
                .saldoCuenta(BigDecimal.TEN)
                .fechaCreacionCuenta(LocalDateTime.now())
                .build();
    }
}