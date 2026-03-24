package org.example.servicios;

import org.example.modelos.Cuenta;
import org.example.modelos.Movimiento;
import org.example.modelos.TipoMovimiento;
import org.example.repositorio.RepositorioCuenta;
import org.example.repositorio.RepositorioMovimiento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovimientoServicioTest {

    @Mock
    private RepositorioCuenta repoCuenta;

    @Mock
    private RepositorioMovimiento repoMovimiento;

    @InjectMocks
    private MovimientoServicio movimientoServicio;

    private Cuenta cuentaOrigen;
    private Cuenta cuentaDestino;

    @BeforeEach
    void setup() {
        cuentaOrigen = new Cuenta(null, "ES1", new BigDecimal("300"), LocalDateTime.now());
        cuentaDestino = new Cuenta(null, "ES2", new BigDecimal("100"), LocalDateTime.now());
    }

    /**
     * Verifica que un depósito válido:
     * - Aumenta el saldo correctamente
     * - Registra un movimiento de tipo DEPOSITO
     */
    @Test
    void depositar_conImportePositivo_debeActualizarSaldoYRegistrarMovimiento() {
        when(repoCuenta.buscarNumeroCuenta("ES1")).thenReturn(cuentaOrigen);

        movimientoServicio.depositar("ES1", new BigDecimal("50"));

        assertEquals(new BigDecimal("350"), cuentaOrigen.getSaldoCuenta());
        verify(repoMovimiento).guardarMovimiento(any(Movimiento.class));
    }

    /**
     * Verifica que no se permite depositar cantidades
     * iguales o inferiores a cero.
     */
    @Test
    void depositar_conImporteCero_debeLanzarExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> movimientoServicio.depositar("ES1", BigDecimal.ZERO));
    }

    /**
     * Verifica que no se puede retirar más dinero
     * del saldo disponible.
     */
    @Test
    void depositar_conCuentaInexistente_debeLanzarExcepcion() {
        when(repoCuenta.buscarNumeroCuenta("ES1")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> movimientoServicio.depositar("ES1", new BigDecimal("50")));
    }

    /**
     * Verifica que un retiro válido:
     * - Disminuye el saldo correctamente
     * - Registra un movimiento de tipo RETIRO
     */
    @Test
    void retirar_conSaldoSuficiente_debeDisminuirSaldoYRegistrarMovimiento() {
        when(repoCuenta.buscarNumeroCuenta("ES1")).thenReturn(cuentaOrigen);

        movimientoServicio.retirar("ES1", new BigDecimal("100"));

        assertEquals(new BigDecimal("200"), cuentaOrigen.getSaldoCuenta());
        verify(repoMovimiento).guardarMovimiento(any(Movimiento.class));
    }

    /**
     * Verifica que no se puede retirar una cantidad
     * superior al saldo disponible.
     */
    @Test
    void retirar_conSaldoInsuficiente_debeLanzarExcepcion() {
        cuentaOrigen.setSaldoCuenta(new BigDecimal("50"));
        when(repoCuenta.buscarNumeroCuenta("ES1")).thenReturn(cuentaOrigen);

        assertThrows(IllegalArgumentException.class, () -> movimientoServicio.retirar("ES1", new BigDecimal("100")));
    }

    /**
     * Verifica que no se puede realizar un retiro
     * si la cuenta no existe.
     */
    @Test
    void retirar_conCuentaInexistente_debeLanzarExcepcion() {
        when(repoCuenta.buscarNumeroCuenta("ES1")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> movimientoServicio.retirar("ES1", new BigDecimal("100")));
    }

    /**
     * Verifica que una transferencia válida:
     * - Resta saldo en la cuenta origen
     * - Suma saldo en la cuenta destino
     * - Registra dos movimientos (saliente y entrante)
     */
    @Test
    void transferir_correctamente_debeActualizarAmbasCuentasYRegistrarDosMovimientos() {
        when(repoCuenta.buscarNumeroCuenta("ES1")).thenReturn(cuentaOrigen);
        when(repoCuenta.buscarNumeroCuenta("ES2")).thenReturn(cuentaDestino);

        movimientoServicio.transferir("ES1", "ES2", new BigDecimal("100"));

        assertEquals(new BigDecimal("200"), cuentaOrigen.getSaldoCuenta());
        assertEquals(new BigDecimal("200"), cuentaDestino.getSaldoCuenta());

        verify(repoMovimiento, times(2)).guardarMovimiento(any(Movimiento.class));
    }

    /**
     * Verifica que no se permite transferir
     * entre la misma cuenta.
     */
    @Test
    void transferir_conMismaCuenta_debeLanzarExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> movimientoServicio.transferir("ES1", "ES1", new BigDecimal("50")));
    }

    /**
     * Verifica que no se puede transferir una cantidad
     * superior al saldo disponible en la cuenta origen.
     */
    @Test
    void transferir_conSaldoInsuficiente_debeLanzarExcepcion() {
        cuentaOrigen.setSaldoCuenta(new BigDecimal("50"));

        when(repoCuenta.buscarNumeroCuenta("ES1")).thenReturn(cuentaOrigen);
        when(repoCuenta.buscarNumeroCuenta("ES2")).thenReturn(cuentaDestino);

        assertThrows(IllegalArgumentException.class,
                () -> movimientoServicio.transferir("ES1", "ES2", new BigDecimal("100")));
    }

    /**
     * Verifica que no se puede realizar una transferencia
     * si la cuenta origen no existe.
     */
    @Test
    void transferir_conCuentaOrigenInexistente_debeLanzarExcepcion() {
        when(repoCuenta.buscarNumeroCuenta("ES1")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> movimientoServicio.transferir("ES1", "ES2", new BigDecimal("100")));
    }

    /**
     * Verifica que no se puede realizar una transferencia
     * si la cuenta destino no existe.
     */
    @Test
    void transferir_conCuentaDestinoInexistente_debeLanzarExcepcion() {
        when(repoCuenta.buscarNumeroCuenta("ES1")).thenReturn(cuentaOrigen);
        when(repoCuenta.buscarNumeroCuenta("ES2")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> movimientoServicio.transferir("ES1", "ES2", new BigDecimal("100")));
    }
}
