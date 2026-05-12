package com.novabank.operacion.service;

import com.novabank.operacion.client.CuentaServiceClient;
import com.novabank.operacion.dto.AplicarMovimientoRequestDTO;
import com.novabank.operacion.dto.AplicarMovimientoResponseDTO;
import com.novabank.operacion.dto.CuentaOperacionRequestDTO;
import com.novabank.operacion.dto.CuentaResponseDTO;
import com.novabank.operacion.dto.OperacionRequestDTO;
import com.novabank.operacion.dto.TransferenciaDivisaRequestDTO;
import com.novabank.operacion.dto.TransferenciaRequestDTO;
import com.novabank.operacion.exception.ExchangeRateUnavailableException;
import com.novabank.operacion.exception.RemoteResourceNotFoundException;
import com.novabank.operacion.exception.RemoteServiceException;
import com.novabank.operacion.exception.RemoteValidationException;
import com.novabank.operacion.exception.ValidationException;
import com.novabank.operacion.mapper.MovimientoMapper;
import com.novabank.operacion.model.Movimiento;
import com.novabank.operacion.model.TipoMovimiento;
import com.novabank.operacion.repository.MovimientoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OperacionServiceTest {

    private CuentaServiceClient cuentaServiceClient;
    private ExchangeRateService exchangeRateService;
    private MovimientoRepository movimientoRepository;
    private OperacionService service;

    @BeforeEach
    void setUp() {
        cuentaServiceClient = mock(CuentaServiceClient.class);
        exchangeRateService = mock(ExchangeRateService.class);
        movimientoRepository = mock(MovimientoRepository.class);
        service = new OperacionService(cuentaServiceClient, exchangeRateService, movimientoRepository, new MovimientoMapper());
    }

    @Test
    void depositoCorrectoActualizaSaldoYGuardaMovimiento() {
        when(cuentaServiceClient.depositar(eq(10L), any(CuentaOperacionRequestDTO.class)))
                .thenReturn(Mono.just(cuenta(10L, "ES91210000000000000001")));
        when(movimientoRepository.save(any(Movimiento.class))).thenAnswer(invocation -> {
            Movimiento movimiento = invocation.getArgument(0);
            movimiento.setId(1L);
            movimiento.setFecha(LocalDateTime.now());
            return Mono.just(movimiento);
        });

        StepVerifier.create(service.depositar(new OperacionRequestDTO(10L, new BigDecimal("50.00"))))
                .assertNext(response -> {
                    assertThat(response.tipoOperacion()).isEqualTo("DEPOSITO");
                    assertThat(response.mensaje()).isEqualTo("Deposito realizado correctamente");
                    assertThat(response.movimientos()).hasSize(1);
                    assertThat(response.movimientos().get(0).tipo()).isEqualTo("DEPOSITO");
                })
                .verifyComplete();

        verify(cuentaServiceClient).depositar(eq(10L), any(CuentaOperacionRequestDTO.class));
        verify(movimientoRepository).save(any(Movimiento.class));
    }

    @Test
    void retiroCorrectoActualizaSaldoYGuardaMovimiento() {
        when(cuentaServiceClient.retirar(eq(10L), any(CuentaOperacionRequestDTO.class)))
                .thenReturn(Mono.just(cuenta(10L, "ES91210000000000000001")));
        when(movimientoRepository.save(any(Movimiento.class))).thenAnswer(invocation -> {
            Movimiento movimiento = invocation.getArgument(0);
            movimiento.setId(2L);
            movimiento.setFecha(LocalDateTime.now());
            return Mono.just(movimiento);
        });

        StepVerifier.create(service.retirar(new OperacionRequestDTO(10L, new BigDecimal("25.00"))))
                .assertNext(response -> {
                    assertThat(response.tipoOperacion()).isEqualTo("RETIRO");
                    assertThat(response.movimientos()).hasSize(1);
                    assertThat(response.movimientos().get(0).tipo()).isEqualTo("RETIRO");
                })
                .verifyComplete();

        verify(cuentaServiceClient).retirar(eq(10L), any(CuentaOperacionRequestDTO.class));
        verify(movimientoRepository).save(any(Movimiento.class));
    }

    @Test
    void transferenciaCorrectaUsaEndpointInternoUnicoYGuardaDosMovimientos() {
        when(cuentaServiceClient.aplicarMovimiento(any(AplicarMovimientoRequestDTO.class)))
                .thenReturn(Mono.just(aplicarMovimientoResponse()));
        AtomicLong ids = new AtomicLong(10L);
        when(movimientoRepository.save(any(Movimiento.class))).thenAnswer(invocation -> {
            Movimiento movimiento = invocation.getArgument(0);
            movimiento.setId(ids.getAndIncrement());
            movimiento.setFecha(LocalDateTime.now());
            return Mono.just(movimiento);
        });

        StepVerifier.create(service.transferir(new TransferenciaRequestDTO(10L, 11L, new BigDecimal("25.00"))))
                .assertNext(response -> {
                    assertThat(response.tipoOperacion()).isEqualTo("TRANSFERENCIA");
                    assertThat(response.movimientos()).hasSize(2);
                    assertThat(response.movimientos().get(0).tipo()).isEqualTo("TRANSFERENCIA_SALIENTE");
                    assertThat(response.movimientos().get(1).tipo()).isEqualTo("TRANSFERENCIA_ENTRANTE");
                })
                .verifyComplete();

        verify(cuentaServiceClient).aplicarMovimiento(any(AplicarMovimientoRequestDTO.class));
        verify(cuentaServiceClient, never()).retirar(any(), any());
        verify(cuentaServiceClient, never()).depositar(any(), any());
    }

    @Test
    void saldoInsuficientePropagaErrorControladoYNoGuardaMovimiento() {
        when(cuentaServiceClient.retirar(eq(10L), any(CuentaOperacionRequestDTO.class)))
                .thenReturn(Mono.error(new RemoteValidationException("Saldo insuficiente")));

        StepVerifier.create(service.retirar(new OperacionRequestDTO(10L, new BigDecimal("999.00"))))
                .expectError(RemoteValidationException.class)
                .verify();

        verify(movimientoRepository, never()).save(any(Movimiento.class));
    }

    @Test
    void cuentaServiceNoDisponibleEnDepositoPropagaServicioNoDisponible() {
        when(cuentaServiceClient.depositar(eq(10L), any(CuentaOperacionRequestDTO.class)))
                .thenReturn(Mono.error(new RemoteServiceException("cuenta-service no esta disponible")));

        StepVerifier.create(service.depositar(new OperacionRequestDTO(10L, new BigDecimal("50.00"))))
                .expectError(RemoteServiceException.class)
                .verify();
    }

    @Test
    void cuentaServiceNoDisponibleEnTransferenciaPropagaServicioNoDisponible() {
        when(cuentaServiceClient.aplicarMovimiento(any(AplicarMovimientoRequestDTO.class)))
                .thenReturn(Mono.error(new RemoteServiceException("cuenta-service no esta disponible")));

        StepVerifier.create(service.transferir(new TransferenciaRequestDTO(10L, 11L, new BigDecimal("50.00"))))
                .expectError(RemoteServiceException.class)
                .verify();
    }

    @Test
    void cuentaNoEncontradaEnDepositoNoGuardaMovimiento() {
        when(cuentaServiceClient.depositar(eq(99L), any(CuentaOperacionRequestDTO.class)))
                .thenReturn(Mono.error(new RemoteResourceNotFoundException("Cuenta no encontrada")));

        StepVerifier.create(service.depositar(new OperacionRequestDTO(99L, new BigDecimal("10.00"))))
                .expectError(RemoteResourceNotFoundException.class)
                .verify();

        verify(movimientoRepository, never()).save(any(Movimiento.class));
    }

    @Test
    void respuestaRemotaSinCuentaEnDepositoDevuelveErrorControlado() {
        when(cuentaServiceClient.depositar(eq(10L), any(CuentaOperacionRequestDTO.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(service.depositar(new OperacionRequestDTO(10L, new BigDecimal("10.00"))))
                .expectError(RemoteResourceNotFoundException.class)
                .verify();

        verify(movimientoRepository, never()).save(any(Movimiento.class));
    }

    @Test
    void transferenciaSinCuentaDestinoEnRespuestaRemotaNoGuardaMovimientos() {
        when(cuentaServiceClient.aplicarMovimiento(any(AplicarMovimientoRequestDTO.class)))
                .thenReturn(Mono.just(new AplicarMovimientoResponseDTO(
                        "op-test",
                        "COMPLETED",
                        "Operacion aplicada",
                        cuenta(10L, "ES91210000000000000001"),
                        null
                )));

        StepVerifier.create(service.transferir(new TransferenciaRequestDTO(10L, 11L, new BigDecimal("10.00"))))
                .expectError(RemoteResourceNotFoundException.class)
                .verify();

        verify(movimientoRepository, never()).save(any(Movimiento.class));
    }

    @Test
    void listarMovimientosSinFechasUsaOrdenDescendenteDelRepositorio() {
        Movimiento movimiento = movimiento(1L, "DEPOSITO", "10.00", LocalDateTime.now());
        when(movimientoRepository.findByCuentaIdOrderByFechaDesc(10L)).thenReturn(Flux.just(movimiento));

        StepVerifier.create(service.listarMovimientos(10L, null, null))
                .assertNext(response -> assertThat(response.tipo()).isEqualTo("DEPOSITO"))
                .verifyComplete();

        verify(movimientoRepository).findByCuentaIdOrderByFechaDesc(10L);
    }

    @Test
    void listarMovimientosConRangoFiltraPorFechasCompletas() {
        Movimiento movimiento = movimiento(1L, "RETIRO", "5.00", LocalDateTime.of(2026, 1, 5, 10, 0));
        when(movimientoRepository.findByCuentaIdAndFechaBetweenOrderByFechaDesc(
                eq(10L),
                eq(LocalDate.of(2026, 1, 1).atStartOfDay()),
                any()
        )).thenReturn(Flux.just(movimiento));

        StepVerifier.create(service.listarMovimientos(10L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
                .assertNext(response -> assertThat(response.tipo()).isEqualTo("RETIRO"))
                .verifyComplete();
    }

    @Test
    void listarMovimientosConSoloUnaFechaLanzaValidationException() {
        StepVerifier.create(service.listarMovimientos(10L, LocalDate.of(2026, 1, 1), null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void listarMovimientosConCuentaIdInvalidoLanzaValidationException() {
        StepVerifier.create(service.listarMovimientos(0L, null, null))
                .expectError(ValidationException.class)
                .verify();
    }

    @Test
    void listarMovimientosConFechaInicioPosteriorLanzaValidationException() {
        StepVerifier.create(service.listarMovimientos(
                        10L,
                        LocalDate.of(2026, 1, 10),
                        LocalDate.of(2026, 1, 1)
                ))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void depositoConDecimalesGuardaCantidadExacta() {
        when(cuentaServiceClient.depositar(eq(10L), any(CuentaOperacionRequestDTO.class)))
                .thenReturn(Mono.just(cuenta(10L, "ES91210000000000000001")));
        when(movimientoRepository.save(any(Movimiento.class))).thenAnswer(invocation -> {
            Movimiento movimiento = invocation.getArgument(0);
            movimiento.setId(3L);
            movimiento.setFecha(LocalDateTime.now());
            return Mono.just(movimiento);
        });

        StepVerifier.create(service.depositar(new OperacionRequestDTO(10L, new BigDecimal("10.50"))))
                .assertNext(response -> assertThat(response.movimientos().get(0).cantidad()).isEqualByComparingTo("10.50"))
                .verifyComplete();
    }

    @Test
    void transferenciaEnDivisaConsultaTasaYEjecutaTransferenciaConMontoConvertido() {
        when(exchangeRateService.obtenerCotizacion("USD", "EUR"))
                .thenReturn(Mono.just(new ExchangeRateQuote(new BigDecimal("0.92"), false)));
        when(cuentaServiceClient.aplicarMovimiento(any(AplicarMovimientoRequestDTO.class)))
                .thenReturn(Mono.just(new AplicarMovimientoResponseDTO(
                        "op-1",
                        "COMPLETED",
                        "Operacion aplicada",
                        cuenta(10L, "ES91210000000000000001"),
                        cuenta(11L, "ES91210000000000000002")
                )));
        AtomicLong ids = new AtomicLong(30L);
        when(movimientoRepository.save(any(Movimiento.class))).thenAnswer(invocation -> {
            Movimiento movimiento = invocation.getArgument(0);
            movimiento.setId(ids.getAndIncrement());
            movimiento.setFecha(LocalDateTime.now());
            return Mono.just(movimiento);
        });

        StepVerifier.create(service.transferirEnDivisa(new TransferenciaDivisaRequestDTO(
                        10L,
                        11L,
                        new BigDecimal("100.00"),
                        "USD",
                        "EUR"
                )))
                .assertNext(response -> {
                    assertThat(response.tipoOperacion()).isEqualTo("TRANSFERENCIA");
                    assertThat(response.movimientos()).hasSize(2);
                    assertThat(response.movimientos().get(0).cantidad()).isEqualByComparingTo("92.00");
                })
                .verifyComplete();

        verify(exchangeRateService).obtenerCotizacion("USD", "EUR");
        verify(cuentaServiceClient).aplicarMovimiento(any(AplicarMovimientoRequestDTO.class));
    }

    @Test
    void transferenciaEnDivisaConTasaCacheadaInformaElUsoDeCache() {
        when(exchangeRateService.obtenerCotizacion("USD", "EUR"))
                .thenReturn(Mono.just(new ExchangeRateQuote(new BigDecimal("0.90"), true)));
        when(cuentaServiceClient.aplicarMovimiento(any(AplicarMovimientoRequestDTO.class)))
                .thenReturn(Mono.just(aplicarMovimientoResponse()));
        AtomicLong ids = new AtomicLong(40L);
        when(movimientoRepository.save(any(Movimiento.class))).thenAnswer(invocation -> {
            Movimiento movimiento = invocation.getArgument(0);
            movimiento.setId(ids.getAndIncrement());
            movimiento.setFecha(LocalDateTime.now());
            return Mono.just(movimiento);
        });

        StepVerifier.create(service.transferirEnDivisa(new TransferenciaDivisaRequestDTO(
                        10L,
                        11L,
                        new BigDecimal("100.00"),
                        "USD",
                        "EUR"
                )))
                .assertNext(response -> {
                    assertThat(response.mensaje()).contains("tasa cacheada");
                    assertThat(response.movimientos().get(0).cantidad()).isEqualByComparingTo("90.00");
                })
                .verifyComplete();
    }

    @Test
    void transferenciaEnDivisaSiFallaTipoCambioNoLlamaCuentaServiceNiGuardaMovimiento() {
        when(exchangeRateService.obtenerCotizacion("USD", "EUR"))
                .thenReturn(Mono.error(new ExchangeRateUnavailableException("No hay tasa fiable")));

        StepVerifier.create(service.transferirEnDivisa(new TransferenciaDivisaRequestDTO(
                        10L,
                        11L,
                        new BigDecimal("100.00"),
                        "USD",
                        "EUR"
                )))
                .expectError(ExchangeRateUnavailableException.class)
                .verify();

        verifyNoInteractions(cuentaServiceClient);
        verify(movimientoRepository, never()).save(any(Movimiento.class));
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

    private AplicarMovimientoResponseDTO aplicarMovimientoResponse() {
        return new AplicarMovimientoResponseDTO(
                "op-test",
                "COMPLETED",
                "Operacion aplicada",
                cuenta(10L, "ES91210000000000000001"),
                cuenta(11L, "ES91210000000000000002")
        );
    }

    private Movimiento movimiento(Long id, String tipo, String cantidad, LocalDateTime fecha) {
        Movimiento movimiento = new Movimiento();
        movimiento.setId(id);
        movimiento.setCuentaId(10L);
        movimiento.setNumeroCuenta("ES91210000000000000001");
        movimiento.setTipo(TipoMovimiento.valueOf(tipo));
        movimiento.setCantidad(new BigDecimal(cantidad));
        movimiento.setFecha(fecha);
        return movimiento;
    }
}
