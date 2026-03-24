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

    // ===============================
    // DEPOSITAR
    // ===============================

    @Test
    void depositar_conImportePositivo_debeActualizarSaldoYRegistrarMovimiento() {
        when(repoCuenta.buscarNumeroCuenta("ES1")).thenReturn(cuentaOrigen);

        movimientoServicio.depositar("ES1", new BigDecimal("50"));

        assertEquals(new BigDecimal("350"), cuentaOrigen.getSaldoCuenta());
        verify(repoMovimiento).guardarMovimiento(any(Movimiento.class));
    }

    @Test
    void depositar_conImporteCero_debeLanzarExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> movimientoServicio.depositar("ES1", BigDecimal.ZERO));
    }

    @Test
    void depositar_conCuentaInexistente_debeLanzarExcepcion() {
        when(repoCuenta.buscarNumeroCuenta("ES1")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> movimientoServicio.depositar("ES1", new BigDecimal("50")));
    }

    // ===============================
    // RETIRAR
    // ===============================

    @Test
    void retirar_conSaldoSuficiente_debeDisminuirSaldoYRegistrarMovimiento() {
        when(repoCuenta.buscarNumeroCuenta("ES1")).thenReturn(cuentaOrigen);

        movimientoServicio.retirar("ES1", new BigDecimal("100"));

        assertEquals(new BigDecimal("200"), cuentaOrigen.getSaldoCuenta());
        verify(repoMovimiento).guardarMovimiento(any(Movimiento.class));
    }

    @Test
    void retirar_conSaldoInsuficiente_debeLanzarExcepcion() {
        cuentaOrigen.setSaldoCuenta(new BigDecimal("50"));
        when(repoCuenta.buscarNumeroCuenta("ES1")).thenReturn(cuentaOrigen);

        assertThrows(IllegalArgumentException.class,
                () -> movimientoServicio.retirar("ES1", new BigDecimal("100")));
    }

    @Test
    void retirar_conCuentaInexistente_debeLanzarExcepcion() {
        when(repoCuenta.buscarNumeroCuenta("ES1")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> movimientoServicio.retirar("ES1", new BigDecimal("100")));
    }

    // ===============================
    // TRANSFERIR
    // ===============================

    @Test
    void transferir_correctamente_debeActualizarAmbasCuentasYRegistrarDosMovimientos() {
        when(repoCuenta.buscarNumeroCuenta("ES1")).thenReturn(cuentaOrigen);
        when(repoCuenta.buscarNumeroCuenta("ES2")).thenReturn(cuentaDestino);

        movimientoServicio.transferir("ES1", "ES2", new BigDecimal("100"));

        assertEquals(new BigDecimal("200"), cuentaOrigen.getSaldoCuenta());
        assertEquals(new BigDecimal("200"), cuentaDestino.getSaldoCuenta());

        verify(repoMovimiento, times(2)).guardarMovimiento(any(Movimiento.class));
    }

    @Test
    void transferir_conMismaCuenta_debeLanzarExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> movimientoServicio.transferir("ES1", "ES1", new BigDecimal("50")));
    }

    @Test
    void transferir_conSaldoInsuficiente_debeLanzarExcepcion() {
        cuentaOrigen.setSaldoCuenta(new BigDecimal("50"));

        when(repoCuenta.buscarNumeroCuenta("ES1")).thenReturn(cuentaOrigen);
        when(repoCuenta.buscarNumeroCuenta("ES2")).thenReturn(cuentaDestino);

        assertThrows(IllegalArgumentException.class,
                () -> movimientoServicio.transferir("ES1", "ES2", new BigDecimal("100")));
    }

    @Test
    void transferir_conCuentaOrigenInexistente_debeLanzarExcepcion() {
        when(repoCuenta.buscarNumeroCuenta("ES1")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> movimientoServicio.transferir("ES1", "ES2", new BigDecimal("100")));
    }

    @Test
    void transferir_conCuentaDestinoInexistente_debeLanzarExcepcion() {
        when(repoCuenta.buscarNumeroCuenta("ES1")).thenReturn(cuentaOrigen);
        when(repoCuenta.buscarNumeroCuenta("ES2")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> movimientoServicio.transferir("ES1", "ES2", new BigDecimal("100")));
    }
}
