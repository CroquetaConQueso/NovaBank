package org.example.servicios;

import org.example.modelos.Cuenta;
import org.example.modelos.Movimiento;
import org.example.repositorio.RepositorioCuenta;
import org.example.repositorio.RepositorioMovimiento;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

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

    // ===============================
    // DEPOSITAR
    // ===============================

    @Test
    void depositar_conImportePositivo_debeActualizarSaldoYRegistrarMovimiento() {
        Cuenta cuenta = new Cuenta();
        cuenta.setNumeroCuenta("ES1");
        cuenta.setSaldoCuenta(new BigDecimal("100"));

        when(repoCuenta.buscarNumeroCuenta("ES1")).thenReturn(cuenta);

        movimientoServicio.depositar("ES1", new BigDecimal("50"));

        assertEquals(new BigDecimal("150"), cuenta.getSaldoCuenta());
        verify(repoMovimiento, times(1)).guardarMovimiento(any(Movimiento.class));
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
                () -> movimientoServicio.depositar("ES1", new BigDecimal("100")));
    }

    // ===============================
    // RETIRAR
    // ===============================

    @Test
    void retirar_conSaldoSuficiente_debeDisminuirSaldoYRegistrarMovimiento() {
        Cuenta cuenta = new Cuenta();
        cuenta.setNumeroCuenta("ES1");
        cuenta.setSaldoCuenta(new BigDecimal("200"));

        when(repoCuenta.buscarNumeroCuenta("ES1")).thenReturn(cuenta);

        movimientoServicio.retirar("ES1", new BigDecimal("50"));

        assertEquals(new BigDecimal("150"), cuenta.getSaldoCuenta());
        verify(repoMovimiento).guardarMovimiento(any(Movimiento.class));
    }

    @Test
    void retirar_conSaldoInsuficiente_debeLanzarExcepcion() {
        Cuenta cuenta = new Cuenta();
        cuenta.setNumeroCuenta("ES1");
        cuenta.setSaldoCuenta(new BigDecimal("20"));

        when(repoCuenta.buscarNumeroCuenta("ES1")).thenReturn(cuenta);

        assertThrows(IllegalArgumentException.class,
                () -> movimientoServicio.retirar("ES1", new BigDecimal("50")));
    }

    @Test
    void retirar_conCuentaInexistente_debeLanzarExcepcion() {
        when(repoCuenta.buscarNumeroCuenta("ES1")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> movimientoServicio.retirar("ES1", new BigDecimal("50")));
    }

    // ===============================
    // TRANSFERIR
    // ===============================

    @Test
    void transferir_correctamente_debeActualizarSaldosYRegistrarDosMovimientos() {
        Cuenta origen = new Cuenta();
        origen.setNumeroCuenta("ES1");
        origen.setSaldoCuenta(new BigDecimal("300"));

        Cuenta destino = new Cuenta();
        destino.setNumeroCuenta("ES2");
        destino.setSaldoCuenta(new BigDecimal("100"));

        when(repoCuenta.buscarNumeroCuenta("ES1")).thenReturn(origen);
        when(repoCuenta.buscarNumeroCuenta("ES2")).thenReturn(destino);

        movimientoServicio.transferir("ES1", "ES2", new BigDecimal("100"));

        assertEquals(new BigDecimal("200"), origen.getSaldoCuenta());
        assertEquals(new BigDecimal("200"), destino.getSaldoCuenta());
        verify(repoMovimiento, times(2)).guardarMovimiento(any(Movimiento.class));
    }

    @Test
    void transferir_conMismaCuenta_debeLanzarExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> movimientoServicio.transferir("ES1", "ES1", new BigDecimal("50")));
    }

    @Test
    void transferir_conSaldoInsuficiente_debeLanzarExcepcion() {
        Cuenta origen = new Cuenta();
        origen.setNumeroCuenta("ES1");
        origen.setSaldoCuenta(new BigDecimal("10"));

        Cuenta destino = new Cuenta();
        destino.setNumeroCuenta("ES2");
        destino.setSaldoCuenta(new BigDecimal("100"));

        when(repoCuenta.buscarNumeroCuenta("ES1")).thenReturn(origen);
        when(repoCuenta.buscarNumeroCuenta("ES2")).thenReturn(destino);

        assertThrows(IllegalArgumentException.class,
                () -> movimientoServicio.transferir("ES1", "ES2", new BigDecimal("50")));
    }

    @Test
    void transferir_conCuentaOrigenInexistente_debeLanzarExcepcion() {
        when(repoCuenta.buscarNumeroCuenta("ES1")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> movimientoServicio.transferir("ES1", "ES2", new BigDecimal("50")));
    }

    @Test
    void transferir_conCuentaDestinoInexistente_debeLanzarExcepcion() {
        Cuenta origen = new Cuenta();
        origen.setNumeroCuenta("ES1");
        origen.setSaldoCuenta(new BigDecimal("200"));

        when(repoCuenta.buscarNumeroCuenta("ES1")).thenReturn(origen);
        when(repoCuenta.buscarNumeroCuenta("ES2")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> movimientoServicio.transferir("ES1", "ES2", new BigDecimal("50")));
    }
}
