package com.novabank.cuenta.service;

import com.novabank.cuenta.dto.AplicarMovimientoRequestDTO;
import com.novabank.cuenta.application.port.out.MovimientoRegistradoPublisherPort;
import com.novabank.cuenta.exception.IdempotencyConflictException;
import com.novabank.cuenta.exception.InsufficientBalanceException;
import com.novabank.cuenta.exception.ResourceNotFoundException;
import com.novabank.cuenta.mapper.CuentaMapper;
import com.novabank.cuenta.model.Cuenta;
import com.novabank.cuenta.model.EstadoOperacionIdempotente;
import com.novabank.cuenta.repository.CuentaRepository;
import com.novabank.cuenta.repository.OperacionIdempotenteRepository;
import com.novabank.cuenta.testsupport.PostgresTestContainerSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DataR2dbcTest
@Import({CuentaMovimientoAtomicoService.class, CuentaMapper.class})
@ActiveProfiles("test")
class CuentaMovimientoAtomicoServiceTest extends PostgresTestContainerSupport {

    @Autowired
    private CuentaMovimientoAtomicoService cuentaMovimientoAtomicoService;

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private OperacionIdempotenteRepository operacionIdempotenteRepository;

    @MockBean
    private MovimientoRegistradoPublisherPort movimientoRegistradoEventPublisher;

    @MockBean
    private SaldoBajoAlertService saldoBajoAlertService;

    @BeforeEach
    void setUp() {
        reset(movimientoRegistradoEventPublisher);
        reset(saldoBajoAlertService);
        org.mockito.Mockito.when(movimientoRegistradoEventPublisher.publicar(any())).thenReturn(Mono.empty());
        org.mockito.Mockito.when(saldoBajoAlertService.evaluarYPublicar(any())).thenReturn(Mono.empty());
        operacionIdempotenteRepository.deleteAll()
                .then(cuentaRepository.deleteAll())
                .block();
    }

    @Test
    void aplicarMovimientoTransfiereSaldoYRegistraIdempotencia() {
        StepVerifier.create(guardarCuentas()
                        .flatMap(cuentas -> cuentaMovimientoAtomicoService.aplicarMovimiento(request(
                                "op-1",
                                cuentas.origenId(),
                                cuentas.destinoId(),
                                "25.00"
                        ))))
                .assertNext(response -> {
                    assertThat(response.operationId()).isEqualTo("op-1");
                    assertThat(response.estado()).isEqualTo("COMPLETED");
                    assertThat(response.cuentaOrigen().saldo()).isEqualByComparingTo("75.00");
                    assertThat(response.cuentaDestino().saldo()).isEqualByComparingTo("125.00");
                })
                .verifyComplete();

        StepVerifier.create(cuentaRepository.findAll().collectList())
                .assertNext(cuentas -> {
                    assertThat(cuentas).extracting(Cuenta::getSaldo)
                            .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                            .containsExactlyInAnyOrder(new BigDecimal("75.00"), new BigDecimal("125.00"));
                })
                .verifyComplete();

        StepVerifier.create(operacionIdempotenteRepository.findByOperationId("op-1"))
                .assertNext(operacion -> {
                    assertThat(operacion.getRequestHash()).hasSize(64);
                    assertThat(operacion.getEstado()).isEqualTo(EstadoOperacionIdempotente.COMPLETED);
                })
                .verifyComplete();
    }

    @Test
    void repetirMismaOperacionNoDuplicaSaldo() {
        Mono<Void> escenario = guardarCuentas()
                .flatMap(cuentas -> cuentaMovimientoAtomicoService.aplicarMovimiento(request(
                                "op-2",
                                cuentas.origenId(),
                                cuentas.destinoId(),
                                "25.00"
                        ))
                        .then(cuentaMovimientoAtomicoService.aplicarMovimiento(request(
                                "op-2",
                                cuentas.origenId(),
                                cuentas.destinoId(),
                                "25.00"
                        ))))
                .then();

        StepVerifier.create(escenario.then(cuentaRepository.findAll().collectList()))
                .assertNext(cuentas -> {
                    assertThat(cuentas).extracting(Cuenta::getSaldo)
                            .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                            .containsExactlyInAnyOrder(new BigDecimal("75.00"), new BigDecimal("125.00"));
                })
                .verifyComplete();
    }

    @Test
    void operacionAtomicaExitosaPublicaEventosParaOrigenYDestino() {
        StepVerifier.create(guardarCuentas()
                        .flatMap(cuentas -> cuentaMovimientoAtomicoService.aplicarMovimiento(request(
                                "op-sse-1",
                                cuentas.origenId(),
                                cuentas.destinoId(),
                                "25.00"
                        ))))
                .expectNextCount(1)
                .verifyComplete();

        verify(movimientoRegistradoEventPublisher, times(2)).publicar(any());
        verify(saldoBajoAlertService, times(2)).evaluarYPublicar(any());
    }

    @Test
    void repeticionIdempotenteNoPublicaEventoDuplicado() {
        StepVerifier.create(guardarCuentas()
                        .flatMap(cuentas -> cuentaMovimientoAtomicoService.aplicarMovimiento(request(
                                        "op-sse-2",
                                        cuentas.origenId(),
                                        cuentas.destinoId(),
                                        "25.00"
                                ))
                                .then(cuentaMovimientoAtomicoService.aplicarMovimiento(request(
                                        "op-sse-2",
                                        cuentas.origenId(),
                                        cuentas.destinoId(),
                                        "25.00"
                                )))))
                .expectNextCount(1)
                .verifyComplete();

        verify(movimientoRegistradoEventPublisher, times(2)).publicar(any());
        verify(saldoBajoAlertService, times(2)).evaluarYPublicar(any());
    }

    @Test
    void repetirOperationIdConBodyDistintoDevuelveConflictoYNoModificaSaldo() {
        StepVerifier.create(guardarCuentas()
                        .flatMap(cuentas -> cuentaMovimientoAtomicoService.aplicarMovimiento(request(
                                        "op-3",
                                        cuentas.origenId(),
                                        cuentas.destinoId(),
                                        "25.00"
                                ))
                                .then(cuentaMovimientoAtomicoService.aplicarMovimiento(request(
                                        "op-3",
                                        cuentas.origenId(),
                                        cuentas.destinoId(),
                                        "10.00"
                                )))))
                .expectError(IdempotencyConflictException.class)
                .verify();

        StepVerifier.create(cuentaRepository.findAll().collectList())
                .assertNext(cuentas -> assertThat(cuentas).extracting(Cuenta::getSaldo)
                        .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                        .containsExactlyInAnyOrder(new BigDecimal("75.00"), new BigDecimal("125.00")))
                .verifyComplete();
    }

    @Test
    void cuentaOrigenInexistenteDevuelve404() {
        StepVerifier.create(guardarCuentaDestino()
                        .flatMap(destino -> cuentaMovimientoAtomicoService.aplicarMovimiento(request(
                                "op-4",
                                9999L,
                                destino.getId(),
                                "25.00"
                        ))))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    void cuentaDestinoInexistenteDevuelve404() {
        StepVerifier.create(guardarCuentaOrigen("100.00")
                        .flatMap(origen -> cuentaMovimientoAtomicoService.aplicarMovimiento(request(
                                "op-5",
                                origen.getId(),
                                9999L,
                                "25.00"
                        ))))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    void saldoInsuficienteNoCambiaSaldosNiRegistraOperacion() {
        StepVerifier.create(guardarCuentas("10.00", "100.00")
                        .flatMap(cuentas -> cuentaMovimientoAtomicoService.aplicarMovimiento(request(
                                "op-6",
                                cuentas.origenId(),
                                cuentas.destinoId(),
                                "25.00"
                        ))))
                .expectError(InsufficientBalanceException.class)
                .verify();

        StepVerifier.create(cuentaRepository.findAll().collectList())
                .assertNext(cuentas -> {
                    assertThat(cuentas).extracting(Cuenta::getSaldo)
                            .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                            .containsExactlyInAnyOrder(new BigDecimal("10.00"), new BigDecimal("100.00"));
                })
                .verifyComplete();

        StepVerifier.create(operacionIdempotenteRepository.findByOperationId("op-6"))
                .verifyComplete();

        verify(saldoBajoAlertService, org.mockito.Mockito.never()).evaluarYPublicar(any());
    }

    @Test
    void montoCeroDevuelveBadRequestAntesDeBuscarCuentas() {
        StepVerifier.create(cuentaMovimientoAtomicoService.aplicarMovimiento(request("op-7", 1L, 2L, "0.00")))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(IllegalArgumentException.class);
                    assertThat(error).hasMessage("El monto debe ser mayor que cero");
                })
                .verify();
    }

    private Mono<CuentasGuardadas> guardarCuentas() {
        return guardarCuentas("100.00", "100.00");
    }

    private Mono<CuentasGuardadas> guardarCuentas(String saldoOrigen, String saldoDestino) {
        return guardarCuentaOrigen(saldoOrigen)
                .zipWith(guardarCuentaDestino(saldoDestino))
                .map(cuentas -> new CuentasGuardadas(cuentas.getT1().getId(), cuentas.getT2().getId()));
    }

    private Mono<Cuenta> guardarCuentaOrigen(String saldo) {
        return cuentaRepository.save(cuenta("ES91210000000000000001", saldo));
    }

    private Mono<Cuenta> guardarCuentaDestino() {
        return guardarCuentaDestino("100.00");
    }

    private Mono<Cuenta> guardarCuentaDestino(String saldo) {
        return cuentaRepository.save(cuenta("ES91210000000000000002", saldo));
    }

    private Cuenta cuenta(String numeroCuenta, String saldo) {
        Cuenta cuenta = new Cuenta();
        cuenta.setClienteId(1L);
        cuenta.setNumeroCuenta(numeroCuenta);
        cuenta.setSaldo(new BigDecimal(saldo));
        cuenta.prepararParaCreacion();
        return cuenta;
    }

    private AplicarMovimientoRequestDTO request(String operationId, Long origenId, Long destinoId, String monto) {
        return new AplicarMovimientoRequestDTO(
                operationId,
                origenId,
                destinoId,
                new BigDecimal(monto),
                "Transferencia interna"
        );
    }

    private record CuentasGuardadas(Long origenId, Long destinoId) {
    }
}
