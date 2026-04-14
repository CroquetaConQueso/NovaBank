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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovimientoServicioTest {

    @Mock
    private CuentaRepository repoCuenta;

    @Mock
    private MovimientoRepository repoMovi;

    @InjectMocks
    private MovimientoServicio movimientoServicio;

    private Cuenta crearCuenta(BigDecimal saldo) {
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
                .saldoCuenta(saldo)
                .fechaCreacionCuenta(LocalDateTime.now())
                .build();
    }

    private Movimiento crearMovimiento(Cuenta cuenta, TipoMovimiento tipo, BigDecimal cantidad) {
        return Movimiento.builder()
                .cuentaAsignada(cuenta)
                .tipoMov(tipo)
                .cantidadMovimiento(cantidad)
                .fechaCreacionMov(LocalDateTime.now())
                .build();
    }

    @Test
    void depositar_conImporteCero_debeLanzarExcepcion() {
        assertThrows(
                ValidationException.class,
                () -> movimientoServicio.depositar("ES12345678901234567890", BigDecimal.ZERO)
        );

        verifyNoInteractions(repoCuenta, repoMovi);
    }

    @Test
    void depositar_conNumeroCuentaInvalido_debeLanzarExcepcion() {
        assertThrows(
                ValidationException.class,
                () -> movimientoServicio.depositar("CUENTA_INVALIDA", BigDecimal.TEN)
        );

        verifyNoInteractions(repoCuenta, repoMovi);
    }

    @Test
    void retirar_conImporteCero_debeLanzarExcepcion() {
        assertThrows(
                ValidationException.class,
                () -> movimientoServicio.retirar("ES12345678901234567890", BigDecimal.ZERO)
        );

        verifyNoInteractions(repoCuenta, repoMovi);
    }

    @Test
    void retirar_conNumeroCuentaInvalido_debeLanzarExcepcion() {
        assertThrows(
                ValidationException.class,
                () -> movimientoServicio.retirar("CUENTA_INVALIDA", BigDecimal.ONE)
        );

        verifyNoInteractions(repoCuenta, repoMovi);
    }

    @Test
    void transferir_conMismaCuenta_debeLanzarExcepcion() {
        assertThrows(
                ValidationException.class,
                () -> movimientoServicio.transferir(
                        "ES11111111111111111111",
                        "ES11111111111111111111",
                        BigDecimal.ONE
                )
        );

        verifyNoInteractions(repoCuenta, repoMovi);
    }

    @Test
    void transferir_conCantidadCero_debeLanzarExcepcion() {
        assertThrows(
                ValidationException.class,
                () -> movimientoServicio.transferir(
                        "ES11111111111111111111",
                        "ES22222222222222222222",
                        BigDecimal.ZERO
                )
        );

        verifyNoInteractions(repoCuenta, repoMovi);
    }

    @Test
    void transferir_conNumeroCuentaOrigenInvalido_debeLanzarExcepcion() {
        assertThrows(
                ValidationException.class,
                () -> movimientoServicio.transferir(
                        "INVALIDA",
                        "ES22222222222222222222",
                        BigDecimal.ONE
                )
        );

        verifyNoInteractions(repoCuenta, repoMovi);
    }

    @Test
    void obtenerLista_conCuentaValida_debeRetornarLista() {
        Cuenta cuenta = crearCuenta(BigDecimal.TEN);
        List<Movimiento> movimientos = List.of(
                crearMovimiento(cuenta, TipoMovimiento.DEPOSITO, BigDecimal.valueOf(20))
        );

        when(repoCuenta.buscarNumeroCuenta("ES12345678901234567890"))
                .thenReturn(Optional.of(cuenta));
        when(repoMovi.obtenerMovimientosCuenta("ES12345678901234567890"))
                .thenReturn(movimientos);

        List<Movimiento> resultado = movimientoServicio.obtenerLista("ES12345678901234567890");

        assertEquals(1, resultado.size());
        verify(repoMovi).obtenerMovimientosCuenta("ES12345678901234567890");
    }

    @Test
    void obtenerLista_conCuentaInexistente_debeLanzarExcepcion() {
        when(repoCuenta.buscarNumeroCuenta("ES12345678901234567890"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> movimientoServicio.obtenerLista("ES12345678901234567890")
        );

        verify(repoMovi, never()).obtenerMovimientosCuenta(anyString());
    }

    @Test
    void obtenerListaFecha_conCuentaValida_debeRetornarLista() {
        Cuenta cuenta = crearCuenta(BigDecimal.TEN);
        LocalDate inicio = LocalDate.now().minusDays(1);
        LocalDate fin = LocalDate.now().plusDays(1);

        List<Movimiento> movimientos = List.of(
                crearMovimiento(cuenta, TipoMovimiento.RETIRO, BigDecimal.valueOf(10))
        );

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
    void obtenerListaFecha_conCuentaInexistente_debeLanzarExcepcion() {
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

    @Test
    void obtenerListaFecha_conFechasInvalidas_debeLanzarExcepcion() {
        Cuenta cuenta = crearCuenta(BigDecimal.TEN);

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
}