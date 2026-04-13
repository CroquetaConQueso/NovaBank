package com.novabank.service;

import com.novabank.domain.model.Cuenta;
import com.novabank.domain.model.Movimiento;
import com.novabank.persistence.memory.RepositorioCuenta;
import com.novabank.persistence.memory.RepositorioMovimiento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultasMovimientoTest {

    @Mock
    private RepositorioCuenta repoCuenta;

    @Mock
    private RepositorioMovimiento repoMovimiento;

    @InjectMocks
    private MovimientoServicio movimientoServicio;

    private Cuenta cuenta;

    @BeforeEach
    void setup() {
        cuenta = new Cuenta(null, "ES1",
                BigDecimal.ZERO, LocalDateTime.now());
    }

    /**
     * Verifica que se lanza excepción cuando
     * se intentan consultar movimientos de una cuenta inexistente.
     */
    @Test
    void obtenerLista_cuentaInexistente_debeLanzarExcepcion() {
        when(repoCuenta.buscarNumeroCuenta("ES1")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> movimientoServicio.obtenerLista("ES1"));
    }

    /**
     * Verifica que se retorna correctamente la lista de movimientos
     * cuando la cuenta existe.
     */
    @Test
    void obtenerLista_cuentaValida_debeRetornarLista() {
        when(repoCuenta.buscarNumeroCuenta("ES1")).thenReturn(cuenta);
        when(repoMovimiento.obtenerMovimientosCuenta("ES1"))
                .thenReturn(List.of());

        List<Movimiento> lista =
                movimientoServicio.obtenerLista("ES1");

        assertNotNull(lista);
        verify(repoMovimiento).obtenerMovimientosCuenta("ES1");
    }

    /**
     * Verifica que se lanza excepción cuando
     * la cuenta no existe al consultar por rango de fechas.
     */
    @Test
    void obtenerListaFecha_cuentaInexistente_debeLanzarExcepcion() {
        when(repoCuenta.buscarNumeroCuenta("ES1")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> movimientoServicio.obtenerListaFecha(
                        "ES1",
                        LocalDate.now(),
                        LocalDate.now()
                ));
    }

    /**
     * Verifica que se lanza excepción cuando
     * la fecha inicial es posterior a la fecha final.
     */
    @Test
    void obtenerListaFecha_fechasInvalidas_debeLanzarExcepcion() {
        when(repoCuenta.buscarNumeroCuenta("ES1")).thenReturn(cuenta);

        assertThrows(IllegalArgumentException.class,
                () -> movimientoServicio.obtenerListaFecha(
                        "ES1",
                        LocalDate.of(2024, 3, 15),
                        LocalDate.of(2024, 3, 1)
                ));
    }

    /**
     * Verifica que se retorna correctamente la lista de movimientos
     * cuando la cuenta existe y el rango de fechas es válido.
     */
    @Test
    void obtenerListaFecha_correcto_debeRetornarLista() {
        when(repoCuenta.buscarNumeroCuenta("ES1")).thenReturn(cuenta);
        when(repoMovimiento.obtenerMovimientosFecha(eq("ES1"), any(), any()))
                .thenReturn(List.of());

        List<Movimiento> lista =
                movimientoServicio.obtenerListaFecha("ES1",
                        LocalDate.of(2024, 3, 1),
                        LocalDate.of(2024, 3, 15));

        assertNotNull(lista);
        verify(repoMovimiento)
                .obtenerMovimientosFecha(eq("ES1"), any(), any());
    }
}
