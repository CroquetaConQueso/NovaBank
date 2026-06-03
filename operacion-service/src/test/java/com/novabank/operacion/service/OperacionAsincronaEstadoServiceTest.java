package com.novabank.operacion.service;

import com.novabank.events.operacion.OperacionCompletadaEvent;
import com.novabank.events.operacion.OperacionFallidaEvent;
import com.novabank.events.operacion.OperacionSolicitadaEvent;
import com.novabank.operacion.model.EstadoOperacionAsincrona;
import com.novabank.operacion.model.OperacionAsincrona;
import com.novabank.operacion.repository.OperacionAsincronaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperacionAsincronaEstadoServiceTest {

    private OperacionAsincronaRepository repository;
    private OperacionAsincronaEstadoService service;

    @BeforeEach
    void setUp() {
        repository = mock(OperacionAsincronaRepository.class);
        service = new OperacionAsincronaEstadoService(repository);
    }

    @Test
    void crearSolicitadaPersisteEstadoInicial() {
        OperacionSolicitadaEvent event = solicitud("DEPOSITO");
        when(repository.save(any(OperacionAsincrona.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.crearSolicitada(event, 10L))
                .assertNext(operacion -> {
                    assertThat(operacion.getOperationId()).isEqualTo(event.operationId());
                    assertThat(operacion.getCorrelationId()).isEqualTo(event.correlationId());
                    assertThat(operacion.getEstado()).isEqualTo(EstadoOperacionAsincrona.SOLICITADA);
                    assertThat(operacion.getCuentaId()).isEqualTo(10L);
                    assertThat(operacion.getCuentaDestinoId()).isEqualTo(10L);
                    assertThat(operacion.getImporte()).isEqualByComparingTo("50.00");
                })
                .verifyComplete();
    }

    @Test
    void marcarCompletadaActualizaOperacionExistente() {
        OperacionAsincrona operacion = operacion(EstadoOperacionAsincrona.SOLICITADA);
        when(repository.findById(operacion.getOperationId())).thenReturn(Mono.just(operacion));
        when(repository.save(any(OperacionAsincrona.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.marcarCompletada(completada(operacion.getOperationId())))
                .verifyComplete();

        ArgumentCaptor<OperacionAsincrona> captor = ArgumentCaptor.forClass(OperacionAsincrona.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoOperacionAsincrona.COMPLETADA);
        assertThat(captor.getValue().getMotivoFallo()).isNull();
    }

    @Test
    void marcarFallidaActualizaOperacionExistente() {
        OperacionAsincrona operacion = operacion(EstadoOperacionAsincrona.SOLICITADA);
        when(repository.findById(operacion.getOperationId())).thenReturn(Mono.just(operacion));
        when(repository.save(any(OperacionAsincrona.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.marcarFallida(fallida(operacion.getOperationId())))
                .verifyComplete();

        ArgumentCaptor<OperacionAsincrona> captor = ArgumentCaptor.forClass(OperacionAsincrona.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoOperacionAsincrona.FALLIDA);
        assertThat(captor.getValue().getMotivoFallo()).isEqualTo("Saldo insuficiente");
    }

    @Test
    void marcarCompletadaDuplicadaNoVuelveAGuardar() {
        OperacionAsincrona operacion = operacion(EstadoOperacionAsincrona.COMPLETADA);
        when(repository.findById(operacion.getOperationId())).thenReturn(Mono.just(operacion));

        StepVerifier.create(service.marcarCompletada(completada(operacion.getOperationId())))
                .verifyComplete();

        verify(repository, never()).save(any(OperacionAsincrona.class));
    }

    @Test
    void resultadoParaOperacionInexistenteNoRompe() {
        UUID operationId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        when(repository.findById(operationId)).thenReturn(Mono.empty());

        StepVerifier.create(service.marcarFallida(fallida(operationId)))
                .verifyComplete();

        verify(repository, never()).save(any(OperacionAsincrona.class));
    }

    private OperacionSolicitadaEvent solicitud(String tipoOperacion) {
        return new OperacionSolicitadaEvent(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                Instant.parse("2026-06-03T10:15:30Z"),
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                tipoOperacion,
                tipoOperacion.equals("RETIRADA") ? 10L : null,
                tipoOperacion.equals("DEPOSITO") ? 10L : null,
                new BigDecimal("50.00"),
                "EUR"
        );
    }

    private OperacionCompletadaEvent completada(UUID operationId) {
        return new OperacionCompletadaEvent(
                UUID.randomUUID(),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                Instant.now(),
                operationId,
                "DEPOSITO",
                null,
                new BigDecimal("50.00"),
                "EUR"
        );
    }

    private OperacionFallidaEvent fallida(UUID operationId) {
        return new OperacionFallidaEvent(
                UUID.randomUUID(),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                Instant.now(),
                operationId,
                "RETIRADA",
                "SALDO_INSUFICIENTE",
                "Saldo insuficiente"
        );
    }

    private OperacionAsincrona operacion(EstadoOperacionAsincrona estado) {
        return OperacionAsincrona.builder()
                .operationId(UUID.fromString("33333333-3333-3333-3333-333333333333"))
                .correlationId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .tipoOperacion("DEPOSITO")
                .cuentaId(10L)
                .cuentaDestinoId(10L)
                .importe(new BigDecimal("50.00"))
                .moneda("EUR")
                .estado(estado)
                .creadaEn(LocalDateTime.now())
                .actualizadaEn(LocalDateTime.now())
                .build();
    }
}
