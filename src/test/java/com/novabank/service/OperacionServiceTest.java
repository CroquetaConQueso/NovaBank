package com.novabank.service;

import com.novabank.dto.MovimientoResponseDTO;
import com.novabank.dto.OperacionRequestDTO;
import com.novabank.dto.TransferenciaRequestDTO;
import com.novabank.exception.InsufficientBalanceException;
import com.novabank.mapper.MovimientoMapper;
import com.novabank.model.Cuenta;
import com.novabank.model.Movimiento;
import com.novabank.model.TipoMovimiento;
import com.novabank.repository.CuentaRepository;
import com.novabank.repository.MovimientoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperacionServiceTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private MovimientoRepository movimientoRepository;

    @Spy
    private MovimientoFactory movimientoFactory;

    @Spy
    private MovimientoMapper movimientoMapper;

    @InjectMocks
    private OperacionService operacionService;

    @Test
    void depositarAumentaSaldoYRegistraMovimiento() {
        Cuenta cuenta = cuenta(1L, "ES00000000000000000001", "100.00");
        when(cuentaRepository.findByNumeroCuenta("ES00000000000000000001")).thenReturn(Optional.of(cuenta));
        when(movimientoRepository.save(any(Movimiento.class))).thenAnswer(invocation -> {
            Movimiento movimiento = invocation.getArgument(0);
            movimiento.setId(20L);
            return movimiento;
        });

        MovimientoResponseDTO response = operacionService.depositar(
                new OperacionRequestDTO("ES00000000000000000001", new BigDecimal("50.00"))
        );

        assertThat(cuenta.getSaldo()).isEqualByComparingTo("150.00");
        assertThat(response.tipo()).isEqualTo(TipoMovimiento.DEPOSITO);
        assertThat(response.cantidad()).isEqualByComparingTo("50.00");
        verify(movimientoRepository).save(any(Movimiento.class));
    }

    @Test
    void retirarLanzaErrorSiSaldoInsuficiente() {
        Cuenta cuenta = cuenta(1L, "ES00000000000000000001", "25.00");
        when(cuentaRepository.findByNumeroCuenta("ES00000000000000000001")).thenReturn(Optional.of(cuenta));

        assertThatThrownBy(() -> operacionService.retirar(
                new OperacionRequestDTO("ES00000000000000000001", new BigDecimal("50.00"))
        ))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessage("Saldo insuficiente. Saldo disponible: 25.00 EUR. Importe solicitado: 50.00 EUR.");

        assertThat(cuenta.getSaldo()).isEqualByComparingTo("25.00");
        verify(movimientoRepository, never()).save(any(Movimiento.class));
    }

    @Test
    void depositarLanzaErrorSiNumeroCuentaEsNulo() {
        assertThatThrownBy(() -> operacionService.depositar(
                new OperacionRequestDTO(null, new BigDecimal("50.00"))
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El numero de cuenta es obligatorio");
    }

    @Test
    void depositarLanzaErrorSiCantidadNoEsPositiva() {
        assertThatThrownBy(() -> operacionService.depositar(
                new OperacionRequestDTO("ES91210000000000000001", BigDecimal.ZERO)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La cantidad debe ser mayor que cero");
    }

    @Test
    void listarMovimientosPorRangoValidaFechas() {
        assertThatThrownBy(() -> operacionService.listarMovimientos(
                1L,
                LocalDate.of(2026, 4, 26),
                LocalDate.of(2026, 4, 1)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("fechaInicio no puede ser posterior a fechaFin");
    }

    @Test
    void listarMovimientosPorRangoFiltraRepositorio() {
        Cuenta cuenta = cuenta(1L, "ES91210000000000000001", "25.00");
        Movimiento movimiento = Movimiento.builder()
                .id(50L)
                .cuenta(cuenta)
                .tipo(TipoMovimiento.DEPOSITO)
                .cantidad(new BigDecimal("10.00"))
                .fecha(LocalDateTime.of(2026, 4, 20, 10, 0))
                .build();

        when(cuentaRepository.existsById(1L)).thenReturn(true);
        when(movimientoRepository.findByCuentaIdAndFechaBetweenOrderByFechaDesc(
                any(Long.class),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of(movimiento));

        List<MovimientoResponseDTO> movimientos = operacionService.listarMovimientos(
                1L,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 26)
        );

        assertThat(movimientos).hasSize(1);
        assertThat(movimientos.get(0).tipo()).isEqualTo(TipoMovimiento.DEPOSITO);
    }

    @Test
    void transferirActualizaAmbasCuentasYRegistraDosMovimientos() {
        Cuenta origen = cuenta(1L, "ES91210000000000000001", "200.00");
        Cuenta destino = cuenta(2L, "ES91210000000000000002", "10.00");
        AtomicLong ids = new AtomicLong(30L);

        when(cuentaRepository.findByNumeroCuenta("ES91210000000000000001")).thenReturn(Optional.of(origen));
        when(cuentaRepository.findByNumeroCuenta("ES91210000000000000002")).thenReturn(Optional.of(destino));
        when(movimientoRepository.save(any(Movimiento.class))).thenAnswer(invocation -> {
            Movimiento movimiento = invocation.getArgument(0);
            movimiento.setId(ids.getAndIncrement());
            return movimiento;
        });

        List<MovimientoResponseDTO> movimientos = operacionService.transferir(
                new TransferenciaRequestDTO(
                        "ES91210000000000000001",
                        "ES91210000000000000002",
                        new BigDecimal("75.00")
                )
        );

        assertThat(origen.getSaldo()).isEqualByComparingTo("125.00");
        assertThat(destino.getSaldo()).isEqualByComparingTo("85.00");
        assertThat(movimientos).hasSize(2);
        assertThat(movimientos.get(0).tipo()).isEqualTo(TipoMovimiento.TRANSFERENCIA_SALIENTE);
        assertThat(movimientos.get(1).tipo()).isEqualTo(TipoMovimiento.TRANSFERENCIA_ENTRANTE);
    }

    private Cuenta cuenta(Long id, String numeroCuenta, String saldo) {
        return Cuenta.builder()
                .id(id)
                .numeroCuenta(numeroCuenta)
                .saldo(new BigDecimal(saldo))
                .build();
    }
}
