package com.novabank.service;

import com.novabank.domain.model.Cliente;
import com.novabank.domain.model.Cuenta;
import com.novabank.domain.model.Movimiento;
import com.novabank.domain.model.TipoMovimiento;
import com.novabank.exception.ResourceNotFoundException;
import com.novabank.exception.ValidationException;
import com.novabank.persistence.memory.RepositorioCuenta;
import com.novabank.persistence.memory.RepositorioMovimiento;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para las consultas de movimientos expuestas por MovimientoServicio.
 */
@ExtendWith(MockitoExtension.class)
class ConsultasMovimientoTest {

    @Mock
    private RepositorioCuenta repoCuenta;

    @Mock
    private RepositorioMovimiento repoMovimiento;

    @InjectMocks
    private MovimientoServicio movimientoServicio;

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

    @Test
    void obtenerLista_cuentaInexistente_debeLanzarExcepcion() {
        when(repoCuenta.buscarNumeroCuenta("ES12345678901234567890")).thenReturn(null);

        assertThrows(
                ResourceNotFoundException.class,
                () -> movimientoServicio.obtenerLista("ES12345678901234567890")
        );
    }

    @Test
    void obtenerLista_cuentaValida_debeRetornarLista() {
        Cuenta cuenta = crearCuenta();
        Movimiento movimiento = Movimiento.builder()
                .cuentaAsignada(cuenta)
                .tipoMov(TipoMovimiento.DEPOSITO)
                .cantidadMovimiento(BigDecimal.TEN)
                .fechaCreacionMov(LocalDateTime.now())
                .build();

        when(repoCuenta.buscarNumeroCuenta("ES12345678901234567890")).thenReturn(cuenta);
        when(repoMovimiento.obtenerMovimientosCuenta("ES12345678901234567890")).thenReturn(List.of(movimiento));

        List<Movimiento> resultado = movimientoServicio.obtenerLista("ES12345678901234567890");

        assertEquals(1, resultado.size());
    }

    @Test
    void obtenerListaFecha_cuentaInexistente_debeLanzarExcepcion() {
        when(repoCuenta.buscarNumeroCuenta("ES12345678901234567890")).thenReturn(null);

        assertThrows(
                ResourceNotFoundException.class,
                () -> movimientoServicio.obtenerListaFecha(
                        "ES12345678901234567890",
                        LocalDate.now().minusDays(5),
                        LocalDate.now()
                )
        );
    }

    @Test
    void obtenerListaFecha_fechasInvalidas_debeLanzarExcepcion() {
        Cuenta cuenta = crearCuenta();
        when(repoCuenta.buscarNumeroCuenta("ES12345678901234567890")).thenReturn(cuenta);

        assertThrows(
                ValidationException.class,
                () -> movimientoServicio.obtenerListaFecha(
                        "ES12345678901234567890",
                        LocalDate.now(),
                        LocalDate.now().minusDays(2)
                )
        );
    }

    @Test
    void obtenerListaFecha_correcto_debeRetornarLista() {
        Cuenta cuenta = crearCuenta();
        Movimiento movimiento = Movimiento.builder()
                .cuentaAsignada(cuenta)
                .tipoMov(TipoMovimiento.DEPOSITO)
                .cantidadMovimiento(BigDecimal.TEN)
                .fechaCreacionMov(LocalDateTime.now())
                .build();

        when(repoCuenta.buscarNumeroCuenta("ES12345678901234567890")).thenReturn(cuenta);
        when(repoMovimiento.obtenerMovimientosFecha(
                "ES12345678901234567890",
                LocalDate.now().minusDays(5),
                LocalDate.now()
        )).thenReturn(List.of(movimiento));

        List<Movimiento> resultado = movimientoServicio.obtenerListaFecha(
                "ES12345678901234567890",
                LocalDate.now().minusDays(5),
                LocalDate.now()
        );

        assertEquals(1, resultado.size());
    }
}