package com.novabank.operacion.service;

import com.novabank.operacion.client.CuentaServiceClient;
import com.novabank.operacion.dto.CuentaOperacionRequestDTO;
import com.novabank.operacion.dto.CuentaResponseDTO;
import com.novabank.operacion.dto.OperacionRequestDTO;
import com.novabank.operacion.dto.OperacionResponseDTO;
import com.novabank.operacion.dto.TransferenciaInternaRequestDTO;
import com.novabank.operacion.dto.TransferenciaRequestDTO;
import com.novabank.operacion.exception.RemoteResourceNotFoundException;
import com.novabank.operacion.exception.RemoteServiceException;
import com.novabank.operacion.exception.RemoteValidationException;
import com.novabank.operacion.exception.ValidationException;
import com.novabank.operacion.mapper.MovimientoMapper;
import com.novabank.operacion.model.Movimiento;
import com.novabank.operacion.repository.MovimientoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperacionServiceTest {

    private CuentaServiceClient cuentaServiceClient;
    private MovimientoRepository movimientoRepository;
    private OperacionService service;

    @BeforeEach
    void setUp() {
        cuentaServiceClient = mock(CuentaServiceClient.class);
        movimientoRepository = mock(MovimientoRepository.class);
        service = new OperacionService(cuentaServiceClient, movimientoRepository, new MovimientoMapper());
    }

    @Test
    void depositoCorrectoActualizaSaldoYGuardaMovimiento() {
        when(cuentaServiceClient.depositar(eq(10L), any(CuentaOperacionRequestDTO.class)))
                .thenReturn(cuenta(10L, "ES91210000000000000001"));
        when(movimientoRepository.save(any(Movimiento.class))).thenAnswer(invocation -> {
            Movimiento movimiento = invocation.getArgument(0);
            movimiento.setId(1L);
            movimiento.setFecha(LocalDateTime.now());
            return movimiento;
        });

        OperacionResponseDTO response = service.depositar(
                new OperacionRequestDTO(10L, new BigDecimal("50.00"))
        );

        assertThat(response.tipoOperacion()).isEqualTo("DEPOSITO");
        assertThat(response.mensaje()).isEqualTo("Deposito realizado correctamente");
        assertThat(response.movimientos()).hasSize(1);
        assertThat(response.movimientos().get(0).tipo()).isEqualTo("DEPOSITO");
        verify(cuentaServiceClient).depositar(eq(10L), any(CuentaOperacionRequestDTO.class));
        verify(movimientoRepository).save(any(Movimiento.class));
    }

    @Test
    void retiroCorrectoActualizaSaldoYGuardaMovimiento() {
        when(cuentaServiceClient.retirar(eq(10L), any(CuentaOperacionRequestDTO.class)))
                .thenReturn(cuenta(10L, "ES91210000000000000001"));
        when(movimientoRepository.save(any(Movimiento.class))).thenAnswer(invocation -> {
            Movimiento movimiento = invocation.getArgument(0);
            movimiento.setId(2L);
            movimiento.setFecha(LocalDateTime.now());
            return movimiento;
        });

        OperacionResponseDTO response = service.retirar(
                new OperacionRequestDTO(10L, new BigDecimal("25.00"))
        );

        assertThat(response.tipoOperacion()).isEqualTo("RETIRO");
        assertThat(response.movimientos()).hasSize(1);
        assertThat(response.movimientos().get(0).tipo()).isEqualTo("RETIRO");
        verify(cuentaServiceClient).retirar(eq(10L), any(CuentaOperacionRequestDTO.class));
        verify(movimientoRepository).save(any(Movimiento.class));
    }

    @Test
    void transferenciaCorrectaUsaEndpointInternoUnicoYGuardaDosMovimientos() {
        when(cuentaServiceClient.transferir(any(TransferenciaInternaRequestDTO.class)))
                .thenReturn(List.of(
                        cuenta(10L, "ES91210000000000000001"),
                        cuenta(11L, "ES91210000000000000002")
                ));
        AtomicLong ids = new AtomicLong(10L);
        when(movimientoRepository.save(any(Movimiento.class))).thenAnswer(invocation -> {
            Movimiento movimiento = invocation.getArgument(0);
            movimiento.setId(ids.getAndIncrement());
            movimiento.setFecha(LocalDateTime.now());
            return movimiento;
        });

        OperacionResponseDTO response = service.transferir(
                new TransferenciaRequestDTO(10L, 11L, new BigDecimal("25.00"))
        );

        assertThat(response.tipoOperacion()).isEqualTo("TRANSFERENCIA");
        assertThat(response.movimientos()).hasSize(2);
        assertThat(response.movimientos().get(0).tipo()).isEqualTo("TRANSFERENCIA_SALIENTE");
        assertThat(response.movimientos().get(1).tipo()).isEqualTo("TRANSFERENCIA_ENTRANTE");
        verify(cuentaServiceClient).transferir(any(TransferenciaInternaRequestDTO.class));
        verify(cuentaServiceClient, never()).retirar(any(), any());
        verify(cuentaServiceClient, never()).depositar(any(), any());
    }

    @Test
    void saldoInsuficientePropagaErrorControladoYNoGuardaMovimiento() {
        when(cuentaServiceClient.retirar(eq(10L), any(CuentaOperacionRequestDTO.class)))
                .thenThrow(new RemoteValidationException("Saldo insuficiente"));

        assertThatThrownBy(() -> service.retirar(
                new OperacionRequestDTO(10L, new BigDecimal("999.00"))
        ))
                .isInstanceOf(RemoteValidationException.class)
                .hasMessageContaining("Saldo insuficiente");

        verify(movimientoRepository, never()).save(any(Movimiento.class));
    }

    @Test
    void cuentaServiceNoDisponibleEnDepositoPropagaServicioNoDisponible() {
        when(cuentaServiceClient.depositar(eq(10L), any(CuentaOperacionRequestDTO.class)))
                .thenThrow(new RemoteServiceException("cuenta-service no esta disponible"));

        assertThatThrownBy(() -> service.depositar(
                new OperacionRequestDTO(10L, new BigDecimal("50.00"))
        ))
                .isInstanceOf(RemoteServiceException.class)
                .hasMessageContaining("cuenta-service no esta disponible");
    }

    @Test
    void cuentaServiceNoDisponibleEnTransferenciaPropagaServicioNoDisponible() {
        when(cuentaServiceClient.transferir(any(TransferenciaInternaRequestDTO.class)))
                .thenThrow(new RemoteServiceException("cuenta-service no esta disponible"));

        assertThatThrownBy(() -> service.transferir(
                new TransferenciaRequestDTO(10L, 11L, new BigDecimal("50.00"))
        ))
                .isInstanceOf(RemoteServiceException.class)
                .hasMessageContaining("cuenta-service no esta disponible");
    }

    @Test
    void cuentaNoEncontradaEnDepositoNoGuardaMovimiento() {
        when(cuentaServiceClient.depositar(eq(99L), any(CuentaOperacionRequestDTO.class)))
                .thenThrow(new RemoteResourceNotFoundException("Cuenta no encontrada"));

        assertThatThrownBy(() -> service.depositar(
                new OperacionRequestDTO(99L, new BigDecimal("10.00"))
        ))
                .isInstanceOf(RemoteResourceNotFoundException.class);

        verify(movimientoRepository, never()).save(any(Movimiento.class));
    }

    @Test
    void respuestaRemotaSinCuentaEnDepositoDevuelveErrorControlado() {
        when(cuentaServiceClient.depositar(eq(10L), any(CuentaOperacionRequestDTO.class)))
                .thenReturn(null);

        assertThatThrownBy(() -> service.depositar(
                new OperacionRequestDTO(10L, new BigDecimal("10.00"))
        ))
                .isInstanceOf(RemoteResourceNotFoundException.class)
                .hasMessageContaining("no devolvio datos");

        verify(movimientoRepository, never()).save(any(Movimiento.class));
    }

    @Test
    void transferenciaSinCuentaDestinoEnRespuestaRemotaNoGuardaMovimientos() {
        when(cuentaServiceClient.transferir(any(TransferenciaInternaRequestDTO.class)))
                .thenReturn(List.of(cuenta(10L, "ES91210000000000000001")));

        assertThatThrownBy(() -> service.transferir(
                new TransferenciaRequestDTO(10L, 11L, new BigDecimal("10.00"))
        ))
                .isInstanceOf(RemoteResourceNotFoundException.class)
                .hasMessageContaining("11");

        verify(movimientoRepository, never()).save(any(Movimiento.class));
    }

    @Test
    void listarMovimientosSinFechasUsaOrdenDescendenteDelRepositorio() {
        Movimiento movimiento = movimiento(1L, "DEPOSITO", "10.00", LocalDateTime.now());
        when(movimientoRepository.findByCuentaIdOrderByFechaDesc(10L)).thenReturn(List.of(movimiento));

        var movimientos = service.listarMovimientos(10L, null, null);

        assertThat(movimientos).hasSize(1);
        assertThat(movimientos.get(0).tipo()).isEqualTo("DEPOSITO");
        verify(movimientoRepository).findByCuentaIdOrderByFechaDesc(10L);
    }

    @Test
    void listarMovimientosConRangoFiltraPorFechasCompletas() {
        Movimiento movimiento = movimiento(1L, "RETIRO", "5.00", LocalDateTime.of(2026, 1, 5, 10, 0));
        when(movimientoRepository.findByCuentaIdAndFechaBetweenOrderByFechaDesc(
                eq(10L),
                eq(LocalDate.of(2026, 1, 1).atStartOfDay()),
                any()
        )).thenReturn(List.of(movimiento));

        var movimientos = service.listarMovimientos(10L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertThat(movimientos).hasSize(1);
        assertThat(movimientos.get(0).tipo()).isEqualTo("RETIRO");
    }

    @Test
    void listarMovimientosConSoloUnaFechaLanzaValidationException() {
        assertThatThrownBy(() -> service.listarMovimientos(10L, LocalDate.of(2026, 1, 1), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fechaInicio y fechaFin");
    }

    @Test
    void listarMovimientosConCuentaIdInvalidoLanzaValidationException() {
        assertThatThrownBy(() -> service.listarMovimientos(0L, null, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("positivo");
    }

    @Test
    void listarMovimientosConFechaInicioPosteriorLanzaValidationException() {
        assertThatThrownBy(() -> service.listarMovimientos(
                10L,
                LocalDate.of(2026, 1, 10),
                LocalDate.of(2026, 1, 1)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fechaInicio");
    }

    @Test
    void depositoConDecimalesGuardaCantidadExacta() {
        when(cuentaServiceClient.depositar(eq(10L), any(CuentaOperacionRequestDTO.class)))
                .thenReturn(cuenta(10L, "ES91210000000000000001"));
        when(movimientoRepository.save(any(Movimiento.class))).thenAnswer(invocation -> {
            Movimiento movimiento = invocation.getArgument(0);
            movimiento.setId(3L);
            movimiento.setFecha(LocalDateTime.now());
            return movimiento;
        });

        OperacionResponseDTO response = service.depositar(
                new OperacionRequestDTO(10L, new BigDecimal("10.50"))
        );

        assertThat(response.movimientos().get(0).cantidad()).isEqualByComparingTo("10.50");
    }

    private CuentaResponseDTO cuenta(Long id, String numeroCuenta) {
        return new CuentaResponseDTO(
                id,
                numeroCuenta,
                1L,
                new BigDecimal("100.00"),
                LocalDateTime.now()
        );
    }

    private Movimiento movimiento(Long id, String tipo, String cantidad, LocalDateTime fecha) {
        Movimiento movimiento = new Movimiento();
        movimiento.setId(id);
        movimiento.setCuentaId(10L);
        movimiento.setNumeroCuenta("ES91210000000000000001");
        movimiento.setTipo(com.novabank.operacion.model.TipoMovimiento.valueOf(tipo));
        movimiento.setCantidad(new BigDecimal(cantidad));
        movimiento.setFecha(fecha);
        return movimiento;
    }
}
