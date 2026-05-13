package com.novabank.operacion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novabank.operacion.client.CuentaServiceClient;
import com.novabank.operacion.dto.AplicarMovimientoRequestDTO;
import com.novabank.operacion.dto.AplicarMovimientoResponseDTO;
import com.novabank.operacion.dto.CuentaOperacionRequestDTO;
import com.novabank.operacion.dto.CuentaResponseDTO;
import com.novabank.operacion.dto.ExchangeRateResultDTO;
import com.novabank.operacion.dto.OperacionRequestDTO;
import com.novabank.operacion.dto.OperacionResponseDTO;
import com.novabank.operacion.dto.TransferenciaDivisaRequestDTO;
import com.novabank.operacion.dto.TransferenciaRequestDTO;
import com.novabank.operacion.exception.ExchangeRateUnavailableException;
import com.novabank.operacion.exception.PublicIdempotencyConflictException;
import com.novabank.operacion.exception.RemoteResourceNotFoundException;
import com.novabank.operacion.exception.RemoteServiceException;
import com.novabank.operacion.exception.RemoteValidationException;
import com.novabank.operacion.exception.ValidationException;
import com.novabank.operacion.mapper.MovimientoMapper;
import com.novabank.operacion.model.EstadoOperacionPublicaIdempotente;
import com.novabank.operacion.model.Movimiento;
import com.novabank.operacion.model.OperacionPublicaIdempotente;
import com.novabank.operacion.model.TipoMovimiento;
import com.novabank.operacion.repository.MovimientoRepository;
import com.novabank.operacion.repository.OperacionPublicaIdempotenteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OperacionServiceTest {

    private CuentaServiceClient cuentaServiceClient;
    private ExchangeRateService exchangeRateService;
    private MovimientoRepository movimientoRepository;
    private OperacionPublicaIdempotenteRepository operacionPublicaIdempotenteRepository;
    private ObjectMapper objectMapper;
    private OperacionService service;

    @BeforeEach
    void setUp() {
        cuentaServiceClient = mock(CuentaServiceClient.class);
        exchangeRateService = mock(ExchangeRateService.class);
        movimientoRepository = mock(MovimientoRepository.class);
        operacionPublicaIdempotenteRepository = mock(OperacionPublicaIdempotenteRepository.class);
        objectMapper = new ObjectMapper().findAndRegisterModules();
        PublicIdempotencyService publicIdempotencyService = new PublicIdempotencyService(
                operacionPublicaIdempotenteRepository,
                objectMapper
        );
        service = new OperacionService(
                cuentaServiceClient,
                exchangeRateService,
                movimientoRepository,
                new MovimientoMapper(),
                publicIdempotencyService
        );
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
        when(exchangeRateService.obtenerTasaConOrigen("USD", "EUR"))
                .thenReturn(Mono.just(new ExchangeRateResultDTO(new BigDecimal("0.92"), false, Instant.now())));
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

        verify(exchangeRateService).obtenerTasaConOrigen("USD", "EUR");
        verify(cuentaServiceClient).aplicarMovimiento(any(AplicarMovimientoRequestDTO.class));
    }

    @Test
    void transferenciaEnDivisaSiFallaTipoCambioNoLlamaCuentaServiceNiGuardaMovimiento() {
        when(exchangeRateService.obtenerTasaConOrigen("USD", "EUR"))
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

    @Test
    void transferenciaEnDivisaConTasaCacheadaMarcaLaRespuesta() {
        when(exchangeRateService.obtenerTasaConOrigen("USD", "EUR"))
                .thenReturn(Mono.just(new ExchangeRateResultDTO(new BigDecimal("0.92"), true, Instant.now())));
        when(cuentaServiceClient.aplicarMovimiento(any(AplicarMovimientoRequestDTO.class)))
                .thenReturn(Mono.just(new AplicarMovimientoResponseDTO(
                        "op-1",
                        "COMPLETED",
                        "Operacion aplicada",
                        cuenta(10L, "ES91210000000000000001"),
                        cuenta(11L, "ES91210000000000000002")
                )));
        when(movimientoRepository.save(any(Movimiento.class))).thenAnswer(invocation -> {
            Movimiento movimiento = invocation.getArgument(0);
            movimiento.setId(70L);
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
                .assertNext(response -> assertThat(response.mensaje()).contains("tasa cacheada"))
                .verifyComplete();
    }

    @Test
    void depositoConIdempotencyKeyNuevaEjecutaUnaVezYGuardaRespuesta() {
        OperacionPublicaIdempotente processing = operacionPublica("deposito-1", "hash", "DEPOSITO");
        when(operacionPublicaIdempotenteRepository.findByIdempotencyKey("deposito-1"))
                .thenReturn(Mono.empty())
                .thenReturn(Mono.just(processing));
        when(operacionPublicaIdempotenteRepository.insertProcessingIfAbsent(eq("deposito-1"), any(), eq("DEPOSITO")))
                .thenReturn(Mono.just(1));
        when(operacionPublicaIdempotenteRepository.save(any(OperacionPublicaIdempotente.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(cuentaServiceClient.depositar(eq(10L), any(CuentaOperacionRequestDTO.class)))
                .thenReturn(Mono.just(cuenta(10L, "ES91210000000000000001")));
        when(movimientoRepository.save(any(Movimiento.class))).thenAnswer(invocation -> {
            Movimiento movimiento = invocation.getArgument(0);
            movimiento.setId(40L);
            movimiento.setFecha(LocalDateTime.now());
            return Mono.just(movimiento);
        });

        StepVerifier.create(service.depositar(new OperacionRequestDTO(10L, new BigDecimal("50.00")), "deposito-1"))
                .assertNext(response -> assertThat(response.tipoOperacion()).isEqualTo("DEPOSITO"))
                .verifyComplete();

        verify(cuentaServiceClient, times(1)).depositar(eq(10L), any(CuentaOperacionRequestDTO.class));
        verify(movimientoRepository, times(1)).save(any(Movimiento.class));
        ArgumentCaptor<OperacionPublicaIdempotente> captor = ArgumentCaptor.forClass(OperacionPublicaIdempotente.class);
        verify(operacionPublicaIdempotenteRepository).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoOperacionPublicaIdempotente.COMPLETED);
        assertThat(captor.getValue().getResponseJson()).contains("DEPOSITO");
    }

    @Test
    void depositoRepetidoConMismaClaveDevuelveRespuestaSinDuplicarSaldoNiMovimiento() throws Exception {
        OperacionResponseDTO response = new OperacionResponseDTO(
                "DEPOSITO",
                "Deposito realizado correctamente",
                List.of(new com.novabank.operacion.dto.MovimientoResponseDTO(
                        40L,
                        10L,
                        "ES91210000000000000001",
                        "DEPOSITO",
                        new BigDecimal("50.00"),
                        LocalDateTime.now()
                ))
        );
        String requestHash = new PublicIdempotencyService(operacionPublicaIdempotenteRepository, objectMapper)
                .hash("DEPOSITO|10|50");
        when(operacionPublicaIdempotenteRepository.findByIdempotencyKey("deposito-1"))
                .thenReturn(Mono.just(operacionCompletada("deposito-1", requestHash, "DEPOSITO", response)));

        StepVerifier.create(service.depositar(new OperacionRequestDTO(10L, new BigDecimal("50.00")), "deposito-1"))
                .assertNext(repeated -> {
                    assertThat(repeated.tipoOperacion()).isEqualTo("DEPOSITO");
                    assertThat(repeated.movimientos()).hasSize(1);
                    assertThat(repeated.movimientos().get(0).id()).isEqualTo(40L);
                })
                .verifyComplete();

        verifyNoInteractions(cuentaServiceClient);
        verify(movimientoRepository, never()).save(any(Movimiento.class));
    }

    @Test
    void depositoRepetidoConMismaClaveYBodyDistintoDevuelveConflicto() {
        when(operacionPublicaIdempotenteRepository.findByIdempotencyKey("deposito-1"))
                .thenReturn(Mono.just(operacionCompletada(
                        "deposito-1",
                        "hash-distinto",
                        "DEPOSITO",
                        new OperacionResponseDTO("DEPOSITO", "ok", List.of())
                )));

        StepVerifier.create(service.depositar(new OperacionRequestDTO(10L, new BigDecimal("50.00")), "deposito-1"))
                .expectError(PublicIdempotencyConflictException.class)
                .verify();

        verifyNoInteractions(cuentaServiceClient);
        verify(movimientoRepository, never()).save(any(Movimiento.class));
    }

    @Test
    void transferenciaConIdempotencyKeyUsaOperationIdEstableEnCuentaService() {
        when(operacionPublicaIdempotenteRepository.findByIdempotencyKey("transferencia-1"))
                .thenReturn(Mono.empty())
                .thenReturn(Mono.just(operacionPublica("transferencia-1", "hash", "TRANSFERENCIA")));
        when(operacionPublicaIdempotenteRepository.insertProcessingIfAbsent(eq("transferencia-1"), any(), eq("TRANSFERENCIA")))
                .thenReturn(Mono.just(1));
        when(operacionPublicaIdempotenteRepository.save(any(OperacionPublicaIdempotente.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(cuentaServiceClient.aplicarMovimiento(any(AplicarMovimientoRequestDTO.class)))
                .thenReturn(Mono.just(aplicarMovimientoResponse()));
        when(movimientoRepository.save(any(Movimiento.class))).thenAnswer(invocation -> {
            Movimiento movimiento = invocation.getArgument(0);
            movimiento.setId(50L);
            movimiento.setFecha(LocalDateTime.now());
            return Mono.just(movimiento);
        });

        StepVerifier.create(service.transferir(
                        new TransferenciaRequestDTO(10L, 11L, new BigDecimal("25.00")),
                        "transferencia-1"
                ))
                .assertNext(response -> assertThat(response.movimientos()).hasSize(2))
                .verifyComplete();

        ArgumentCaptor<AplicarMovimientoRequestDTO> captor = ArgumentCaptor.forClass(AplicarMovimientoRequestDTO.class);
        verify(cuentaServiceClient).aplicarMovimiento(captor.capture());
        assertThat(captor.getValue().operationId()).startsWith("public-");
    }

    @Test
    void transferenciaEnDivisaRepetidaNoConsultaTasaNiTocaCuentaService() {
        OperacionResponseDTO response = new OperacionResponseDTO(
                "TRANSFERENCIA",
                "Transferencia en divisa realizada correctamente con tasa cacheada",
                List.of()
        );
        String requestHash = new PublicIdempotencyService(operacionPublicaIdempotenteRepository, objectMapper)
                .hash("TRANSFERENCIA_DIVISA|10|11|100|USD|EUR");
        when(operacionPublicaIdempotenteRepository.findByIdempotencyKey("divisa-1"))
                .thenReturn(Mono.just(operacionCompletada("divisa-1", requestHash, "TRANSFERENCIA_DIVISA", response)));

        StepVerifier.create(service.transferirEnDivisa(new TransferenciaDivisaRequestDTO(
                        10L,
                        11L,
                        new BigDecimal("100.00"),
                        "USD",
                        "EUR"
                ), "divisa-1"))
                .assertNext(repeated -> assertThat(repeated.mensaje()).contains("tasa cacheada"))
                .verifyComplete();

        verifyNoInteractions(exchangeRateService);
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

    private OperacionPublicaIdempotente operacionPublica(String key, String requestHash, String tipoOperacion) {
        return OperacionPublicaIdempotente.builder()
                .id(1L)
                .idempotencyKey(key)
                .requestHash(requestHash)
                .tipoOperacion(tipoOperacion)
                .estado(EstadoOperacionPublicaIdempotente.PROCESSING)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();
    }

    private OperacionPublicaIdempotente operacionCompletada(
            String key,
            String requestHash,
            String tipoOperacion,
            OperacionResponseDTO response
    ) {
        return OperacionPublicaIdempotente.builder()
                .id(1L)
                .idempotencyKey(key)
                .requestHash(requestHash)
                .tipoOperacion(tipoOperacion)
                .estado(EstadoOperacionPublicaIdempotente.COMPLETED)
                .responseJson(serializar(response))
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();
    }

    private String serializar(OperacionResponseDTO response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
