package com.novabank.service;

import com.novabank.domain.model.Cliente;
import com.novabank.domain.model.Cuenta;
import com.novabank.domain.model.Movimiento;
import com.novabank.domain.model.TipoMovimiento;
import com.novabank.exception.InsufficientBalanceException;
import com.novabank.exception.ResourceNotFoundException;
import com.novabank.exception.ValidationException;
import com.novabank.persistence.repository.CuentaRepository;
import com.novabank.persistence.repository.MovimientoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para MovimientoServicio.
 */
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

    @Test
    void depositar_conCuentaInexistente_debeLanzarExcepcion() {
        when(repoCuenta.buscarNumeroCuenta("ES12345678901234567890"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> movimientoServicio.depositar("ES12345678901234567890", BigDecimal.TEN)
        );
    }

    @Test
    void depositar_conImporteCero_debeLanzarExcepcion() {
        Cuenta cuenta = crearCuenta(BigDecimal.ZERO);

        when(repoCuenta.buscarNumeroCuenta("ES12345678901234567890"))
                .thenReturn(Optional.of(cuenta));

        assertThrows(
                ValidationException.class,
                () -> movimientoServicio.depositar("ES12345678901234567890", BigDecimal.ZERO)
        );
    }

    @Test
    void depositar_conImportePositivo_debeActualizarSaldoYRegistrarMovimiento() {
        Cuenta cuenta = crearCuenta(BigDecimal.TEN);

        when(repoCuenta.buscarNumeroCuenta("ES12345678901234567890"))
                .thenReturn(Optional.of(cuenta));

        movimientoServicio.depositar("ES12345678901234567890", BigDecimal.valueOf(5));

        assertEquals(BigDecimal.valueOf(15), cuenta.getSaldoCuenta());
        verify(repoMovi).guardarMovimiento(any(Movimiento.class));
    }

    @Test
    void retirar_conCuentaInexistente_debeLanzarExcepcion() {
        when(repoCuenta.buscarNumeroCuenta("ES12345678901234567890"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> movimientoServicio.retirar("ES12345678901234567890", BigDecimal.ONE)
        );
    }

    @Test
    void retirar_conSaldoInsuficiente_debeLanzarExcepcion() {
        Cuenta cuenta = crearCuenta(BigDecimal.valueOf(5));

        when(repoCuenta.buscarNumeroCuenta("ES12345678901234567890"))
                .thenReturn(Optional.of(cuenta));

        assertThrows(
                InsufficientBalanceException.class,
                () -> movimientoServicio.retirar("ES12345678901234567890", BigDecimal.TEN)
        );
    }

    @Test
    void retirar_conSaldoSuficiente_debeDisminuirSaldoYRegistrarMovimiento() {
        Cuenta cuenta = crearCuenta(BigDecimal.TEN);

        when(repoCuenta.buscarNumeroCuenta("ES12345678901234567890"))
                .thenReturn(Optional.of(cuenta));

        movimientoServicio.retirar("ES12345678901234567890", BigDecimal.valueOf(4));

        assertEquals(BigDecimal.valueOf(6), cuenta.getSaldoCuenta());
        verify(repoMovi).guardarMovimiento(any(Movimiento.class));
    }

    @Test
    void transferir_conCuentaOrigenInexistente_debeLanzarExcepcion() {
        when(repoCuenta.buscarNumeroCuenta("ES11111111111111111111"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> movimientoServicio.transferir(
                        "ES11111111111111111111",
                        "ES22222222222222222222",
                        BigDecimal.ONE
                )
        );
    }

    @Test
    void transferir_conCuentaDestinoInexistente_debeLanzarExcepcion() {
        Cuenta origen = crearCuenta(BigDecimal.TEN);
        origen.setNumeroCuenta("ES11111111111111111111");

        when(repoCuenta.buscarNumeroCuenta("ES11111111111111111111"))
                .thenReturn(Optional.of(origen));
        when(repoCuenta.buscarNumeroCuenta("ES22222222222222222222"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> movimientoServicio.transferir(
                        "ES11111111111111111111",
                        "ES22222222222222222222",
                        BigDecimal.ONE
                )
        );
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
    }

    @Test
    void transferir_conSaldoInsuficiente_debeLanzarExcepcion() {
        Cuenta origen = crearCuenta(BigDecimal.ONE);
        origen.setNumeroCuenta("ES11111111111111111111");

        Cuenta destino = crearCuenta(BigDecimal.ZERO);
        destino.setNumeroCuenta("ES22222222222222222222");

        when(repoCuenta.buscarNumeroCuenta("ES11111111111111111111"))
                .thenReturn(Optional.of(origen));
        when(repoCuenta.buscarNumeroCuenta("ES22222222222222222222"))
                .thenReturn(Optional.of(destino));

        assertThrows(
                InsufficientBalanceException.class,
                () -> movimientoServicio.transferir(
                        "ES11111111111111111111",
                        "ES22222222222222222222",
                        BigDecimal.TEN
                )
        );
    }

    @Test
    void transferir_correctamente_debeActualizarAmbasCuentasYRegistrarDosMovimientos() {
        Cuenta origen = crearCuenta(BigDecimal.TEN);
        origen.setNumeroCuenta("ES11111111111111111111");

        Cuenta destino = crearCuenta(BigDecimal.ZERO);
        destino.setNumeroCuenta("ES22222222222222222222");

        when(repoCuenta.buscarNumeroCuenta("ES11111111111111111111"))
                .thenReturn(Optional.of(origen));
        when(repoCuenta.buscarNumeroCuenta("ES22222222222222222222"))
                .thenReturn(Optional.of(destino));

        movimientoServicio.transferir(
                "ES11111111111111111111",
                "ES22222222222222222222",
                BigDecimal.valueOf(4)
        );

        assertEquals(BigDecimal.valueOf(6), origen.getSaldoCuenta());
        assertEquals(BigDecimal.valueOf(4), destino.getSaldoCuenta());
        verify(repoMovi, times(2)).guardarMovimiento(any(Movimiento.class));
    }

    @Test
    void depositar_conCuentaInexistente_noDebeRegistrarMovimiento() {
        when(repoCuenta.buscarNumeroCuenta("ES12345678901234567890"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> movimientoServicio.depositar("ES12345678901234567890", BigDecimal.TEN)
        );

        verify(repoMovi, times(0)).guardarMovimiento(any(Movimiento.class));
    }

    @Test
    void depositar_conImportePositivo_debeRegistrarMovimientoConDatosCorrectos() {
        Cuenta cuenta = crearCuenta(BigDecimal.TEN);

        when(repoCuenta.buscarNumeroCuenta("ES12345678901234567890"))
                .thenReturn(Optional.of(cuenta));

        ArgumentCaptor<Movimiento> captor = ArgumentCaptor.forClass(Movimiento.class);

        movimientoServicio.depositar("ES12345678901234567890", BigDecimal.valueOf(5));

        verify(repoMovi).guardarMovimiento(captor.capture());

        Movimiento movimiento = captor.getValue();

        assertEquals(TipoMovimiento.DEPOSITO, movimiento.getTipoMov());
        assertEquals(0, BigDecimal.valueOf(5).compareTo(movimiento.getCantidadMovimiento()));
        assertEquals("ES12345678901234567890", movimiento.getCuentaAsignada().getNumeroCuenta());
    }

    @Test
    void transferir_siFallaElSegundoRegistro_debeRestaurarSaldosEnMemoria() {
        Cuenta origen = crearCuenta(BigDecimal.valueOf(100));
        origen.setNumeroCuenta("ES11111111111111111111");

        Cuenta destino = crearCuenta(BigDecimal.valueOf(50));
        destino.setNumeroCuenta("ES22222222222222222222");

        when(repoCuenta.buscarNumeroCuenta("ES11111111111111111111"))
                .thenReturn(Optional.of(origen));
        when(repoCuenta.buscarNumeroCuenta("ES22222222222222222222"))
                .thenReturn(Optional.of(destino));

        doNothing()
                .doThrow(new RuntimeException("Fallo al registrar el segundo movimiento"))
                .when(repoMovi).guardarMovimiento(any(Movimiento.class));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> movimientoServicio.transferir(
                        "ES11111111111111111111",
                        "ES22222222222222222222",
                        BigDecimal.valueOf(30)
                )
        );

        assertEquals("Error durante la transferencia. Se han revertido los datos en memoria.", exception.getMessage());
        assertEquals(0, BigDecimal.valueOf(100).compareTo(origen.getSaldoCuenta()));
        assertEquals(0, BigDecimal.valueOf(50).compareTo(destino.getSaldoCuenta()));

        verify(repoMovi, times(2)).guardarMovimiento(any(Movimiento.class));
    }

    @Test
    void transferir_conMismaCuenta_noDebeConsultarRepositoriosNiRegistrarMovimientos() {
        assertThrows(
                ValidationException.class,
                () -> movimientoServicio.transferir(
                        "ES11111111111111111111",
                        "ES11111111111111111111",
                        BigDecimal.ONE
                )
        );

        verify(repoCuenta, times(0)).buscarNumeroCuenta(any());
        verify(repoMovi, times(0)).guardarMovimiento(any(Movimiento.class));
    }


}