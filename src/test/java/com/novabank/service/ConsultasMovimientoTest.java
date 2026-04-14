package com.novabank.service;

import com.novabank.domain.model.Cliente;
import com.novabank.domain.model.Cuenta;
import com.novabank.domain.model.Movimiento;
import com.novabank.domain.model.TipoMovimiento;
import com.novabank.exception.ResourceNotFoundException;
import com.novabank.exception.ValidationException;
import com.novabank.persistence.repository.CuentaRepository;
import com.novabank.persistence.repository.MovimientoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para las consultas de movimientos en MovimientoServicio.
 */
@ExtendWith(MockitoExtension.class)
class ConsultasMovimientoTest {

    @Mock
    private CuentaRepository repoCuenta;

    @Mock
    private MovimientoRepository repoMovi;

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

    private Movimiento crearMovimiento(Cuenta cuenta) {
        return Movimiento.builder()
                .cuentaAsignada(cuenta)
                .tipoMov(TipoMovimiento.DEPOSITO)
                .cantidadMovimiento(BigDecimal.valueOf(20))
                .fechaCreacionMov(LocalDateTime.now())
                .build();
    }

    @Test
    void obtenerLista_cuentaValida_debeRetornarLista() {
        Cuenta cuenta = crearCuenta();
        List<Movimiento> movimientos = List.of(crearMovimiento(cuenta));

        when(repoCuenta.buscarNumeroCuenta("ES12345678901234567890"))
                .thenReturn(Optional.of(cuenta));
        when(repoMovi.obtenerMovimientosCuenta("ES12345678901234567890"))
                .thenReturn(movimientos);

        List<Movimiento> resultado = movimientoServicio.obtenerLista("ES12345678901234567890");

        assertEquals(1, resultado.size());
        verify(repoMovi).obtenerMovimientosCuenta("ES12345678901234567890");
    }

    @Test
    void obtenerLista_cuentaInexistente_debeLanzarExcepcion() {
        when(repoCuenta.buscarNumeroCuenta("ES12345678901234567890"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> movimientoServicio.obtenerLista("ES12345678901234567890")
        );

        verify(repoMovi, never()).obtenerMovimientosCuenta(anyString());
    }

    @Test
    void obtenerListaFecha_correcto_debeRetornarLista() {
        Cuenta cuenta = crearCuenta();
        List<Movimiento> movimientos = List.of(crearMovimiento(cuenta));

        LocalDate inicio = LocalDate.now().minusDays(1);
        LocalDate fin = LocalDate.now().plusDays(1);

        when(repoCuenta.buscarNumeroCuenta("ES12345678901234567890"))
                .thenReturn(Optional.of(cuenta));
        when(repoMovi.obtenerMovimientosFecha("ES12345678901234567890", inicio, fin))
                .thenReturn(movimientos);

        List<Movimiento> resultado = movimientoServicio.obtenerListaFecha(
                "ES12345678901234567890",
                inicio,
                fin
        );

        assertEquals(1, resultado.size());
        verify(repoMovi).obtenerMovimientosFecha("ES12345678901234567890", inicio, fin);
    }

    @Test
    void obtenerListaFecha_fechasInvalidas_debeLanzarExcepcion() {
        Cuenta cuenta = crearCuenta();

        when(repoCuenta.buscarNumeroCuenta("ES12345678901234567890"))
                .thenReturn(Optional.of(cuenta));

        assertThrows(
                ValidationException.class,
                () -> movimientoServicio.obtenerListaFecha(
                        "ES12345678901234567890",
                        LocalDate.now().plusDays(1),
                        LocalDate.now()
                )
        );

        verify(repoMovi, never()).obtenerMovimientosFecha(anyString(), any(), any());
    }

    @Test
    void obtenerListaFecha_cuentaInexistente_debeLanzarExcepcion() {
        when(repoCuenta.buscarNumeroCuenta("ES12345678901234567890"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> movimientoServicio.obtenerListaFecha(
                        "ES12345678901234567890",
                        LocalDate.now().minusDays(1),
                        LocalDate.now().plusDays(1)
                )
        );

        verify(repoMovi, never()).obtenerMovimientosFecha(anyString(), any(), any());
    }
}