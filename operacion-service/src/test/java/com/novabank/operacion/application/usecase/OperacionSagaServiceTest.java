package com.novabank.operacion.application.usecase;

import com.novabank.operacion.application.port.in.ActualizarOperacionResultadoCommand;
import com.novabank.operacion.application.port.in.ConsultarEstadoOperacionQuery;
import com.novabank.operacion.application.port.in.SolicitarDepositoCommand;
import com.novabank.operacion.application.port.in.SolicitarRetiradaCommand;
import com.novabank.operacion.application.port.in.SolicitarTransferenciaCommand;
import com.novabank.operacion.application.exception.ComisionNoDisponibleException;
import com.novabank.operacion.application.port.out.ComisionCalculatorPort;
import com.novabank.operacion.application.port.out.OperacionAsincronaRepositoryPort;
import com.novabank.operacion.application.port.out.OperacionSolicitadaPublisherPort;
import com.novabank.operacion.domain.model.EstadoOperacionAsincrona;
import com.novabank.operacion.domain.model.OperacionAsincrona;
import com.novabank.operacion.domain.model.OperacionSolicitada;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperacionSagaServiceTest {

    @Mock
    private OperacionAsincronaRepositoryPort repositoryPort;

    @Mock
    private OperacionSolicitadaPublisherPort publisherPort;

    @Mock
    private ComisionCalculatorPort comisionCalculatorPort;

    @Test
    void solicitarDepositoRegistraSolicitadaPublicaSolicitudYDevuelveAceptada() {
        OperacionSagaService service = service();
        when(repositoryPort.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(publisherPort.publicar(any())).thenReturn(Mono.empty());

        StepVerifier.create(service.solicitarDeposito(new SolicitarDepositoCommand(
                        10L,
                        new BigDecimal("50.00"),
                        null,
                        correlationId()
                )))
                .assertNext(result -> {
                    assertThat(result.estado()).isEqualTo("SOLICITADA");
                    assertThat(result.tipoOperacion()).isEqualTo("DEPOSITO");
                    assertThat(result.cuentaId()).isEqualTo(10L);
                    assertThat(result.importe()).isEqualByComparingTo("50.00");
                })
                .verifyComplete();

        ArgumentCaptor<OperacionSolicitada> captor = ArgumentCaptor.forClass(OperacionSolicitada.class);
        verify(publisherPort).publicar(captor.capture());
        assertThat(captor.getValue().tipoOperacion()).isEqualTo("DEPOSITO");
        assertThat(captor.getValue().cuentaOrigenId()).isNull();
        assertThat(captor.getValue().cuentaDestinoId()).isEqualTo(10L);
        assertThat(captor.getValue().kafkaKey()).isEqualTo(10L);
    }

    @Test
    void solicitarRetiradaRegistraSolicitadaPublicaSolicitudYDevuelveAceptada() {
        OperacionSagaService service = service();
        when(repositoryPort.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(publisherPort.publicar(any())).thenReturn(Mono.empty());

        StepVerifier.create(service.solicitarRetirada(new SolicitarRetiradaCommand(
                        10L,
                        new BigDecimal("25.00"),
                        null,
                        correlationId()
                )))
                .assertNext(result -> {
                    assertThat(result.estado()).isEqualTo("SOLICITADA");
                    assertThat(result.tipoOperacion()).isEqualTo("RETIRADA");
                    assertThat(result.cuentaId()).isEqualTo(10L);
                    assertThat(result.importe()).isEqualByComparingTo("25.00");
                })
                .verifyComplete();

        ArgumentCaptor<OperacionSolicitada> captor = ArgumentCaptor.forClass(OperacionSolicitada.class);
        verify(publisherPort).publicar(captor.capture());
        assertThat(captor.getValue().tipoOperacion()).isEqualTo("RETIRADA");
        assertThat(captor.getValue().cuentaOrigenId()).isEqualTo(10L);
        assertThat(captor.getValue().cuentaDestinoId()).isNull();
        assertThat(captor.getValue().kafkaKey()).isEqualTo(10L);
    }

    @Test
    void solicitarTransferenciaRegistraSolicitadaPublicaSolicitudYDevuelveAceptada() {
        OperacionSagaService service = service();
        when(repositoryPort.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(publisherPort.publicar(any())).thenReturn(Mono.empty());

        StepVerifier.create(service.solicitarTransferencia(new SolicitarTransferenciaCommand(
                        10L,
                        11L,
                        new BigDecimal("25.00"),
                        null,
                        correlationId()
                )))
                .assertNext(result -> {
                    assertThat(result.estado()).isEqualTo("SOLICITADA");
                    assertThat(result.tipoOperacion()).isEqualTo("TRANSFERENCIA");
                    assertThat(result.cuentaOrigenId()).isEqualTo(10L);
                    assertThat(result.cuentaDestinoId()).isEqualTo(11L);
                    assertThat(result.importe()).isEqualByComparingTo("25.00");
                })
                .verifyComplete();

        ArgumentCaptor<OperacionSolicitada> captor = ArgumentCaptor.forClass(OperacionSolicitada.class);
        verify(publisherPort).publicar(captor.capture());
        assertThat(captor.getValue().tipoOperacion()).isEqualTo("TRANSFERENCIA");
        assertThat(captor.getValue().cuentaOrigenId()).isEqualTo(10L);
        assertThat(captor.getValue().cuentaDestinoId()).isEqualTo(11L);
        assertThat(captor.getValue().kafkaKey()).isEqualTo(10L);
        verify(comisionCalculatorPort, never()).calcularComision(any());
    }

    @Test
    void solicitarTransferenciaInternacionalCalculaComisionYPublicaSolicitud() {
        OperacionSagaService service = service();
        when(repositoryPort.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(publisherPort.publicar(any())).thenReturn(Mono.empty());
        when(comisionCalculatorPort.calcularComision(any()))
                .thenReturn(Mono.just(new ComisionCalculada(
                        new BigDecimal("16.00"),
                        new BigDecimal("0.0160"),
                        "US",
                        "EMPRESA"
                )));

        StepVerifier.create(service.solicitarTransferencia(new SolicitarTransferenciaCommand(
                        10L,
                        11L,
                        new BigDecimal("1000.00"),
                        null,
                        correlationId(),
                        true,
                        "US",
                        "EMPRESA"
                )))
                .assertNext(result -> {
                    assertThat(result.estado()).isEqualTo("SOLICITADA");
                    assertThat(result.comision()).isEqualByComparingTo("16.00");
                    assertThat(result.tasaComision()).isEqualByComparingTo("0.0160");
                })
                .verifyComplete();

        ArgumentCaptor<ComisionCommand> comisionCaptor = ArgumentCaptor.forClass(ComisionCommand.class);
        verify(comisionCalculatorPort).calcularComision(comisionCaptor.capture());
        assertThat(comisionCaptor.getValue().importeEuros()).isEqualByComparingTo("1000.00");
        assertThat(comisionCaptor.getValue().paisDestino()).isEqualTo("US");
        assertThat(comisionCaptor.getValue().tipoCliente()).isEqualTo("EMPRESA");
        verify(publisherPort).publicar(any(OperacionSolicitada.class));
    }

    @Test
    void solicitarTransferenciaInternacionalConLambdaFallidaNoPersisteNiPublica() {
        OperacionSagaService service = service();
        when(comisionCalculatorPort.calcularComision(any()))
                .thenReturn(Mono.error(new ComisionNoDisponibleException("Lambda no disponible")));

        StepVerifier.create(service.solicitarTransferencia(new SolicitarTransferenciaCommand(
                        10L,
                        11L,
                        new BigDecimal("1000.00"),
                        null,
                        correlationId(),
                        true,
                        "US",
                        "EMPRESA"
                )))
                .expectError(ComisionNoDisponibleException.class)
                .verify();

        verify(repositoryPort, never()).save(any());
        verify(publisherPort, never()).publicar(any());
    }

    @Test
    void solicitarTransferenciaInternacionalSinPaisDestinoFallaValidacion() {
        OperacionSagaService service = service();

        StepVerifier.create(service.solicitarTransferencia(new SolicitarTransferenciaCommand(
                        10L,
                        11L,
                        new BigDecimal("1000.00"),
                        null,
                        correlationId(),
                        true,
                        " ",
                        "EMPRESA"
                )))
                .expectError(IllegalArgumentException.class)
                .verify();

        verify(comisionCalculatorPort, never()).calcularComision(any());
        verify(repositoryPort, never()).save(any());
        verify(publisherPort, never()).publicar(any());
    }

    @Test
    void solicitarTransferenciaInternacionalSinTipoClienteFallaValidacion() {
        OperacionSagaService service = service();

        StepVerifier.create(service.solicitarTransferencia(new SolicitarTransferenciaCommand(
                        10L,
                        11L,
                        new BigDecimal("1000.00"),
                        null,
                        correlationId(),
                        true,
                        "US",
                        " "
                )))
                .expectError(IllegalArgumentException.class)
                .verify();

        verify(comisionCalculatorPort, never()).calcularComision(any());
        verify(repositoryPort, never()).save(any());
        verify(publisherPort, never()).publicar(any());
    }

    @Test
    void consultarDevuelveEstadoPersistido() {
        OperacionSagaService service = service();
        UUID operationId = UUID.randomUUID();
        when(repositoryPort.findByOperationId(operationId)).thenReturn(Mono.just(operacion(operationId)));

        StepVerifier.create(service.consultar(new ConsultarEstadoOperacionQuery(operationId)))
                .assertNext(result -> {
                    assertThat(result.operationId()).isEqualTo(operationId);
                    assertThat(result.estado()).isEqualTo("SOLICITADA");
                    assertThat(result.tipoOperacion()).isEqualTo("TRANSFERENCIA");
                })
                .verifyComplete();
    }

    @Test
    void actualizarCompletadaMarcaOperacionComoCompletada() {
        OperacionSagaService service = service();
        UUID operationId = UUID.randomUUID();
        when(repositoryPort.findByOperationId(operationId)).thenReturn(Mono.just(operacion(operationId)));
        when(repositoryPort.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.actualizar(new ActualizarOperacionResultadoCommand(
                        operationId,
                        ActualizarOperacionResultadoCommand.Resultado.COMPLETADA,
                        null,
                        null
                )))
                .assertNext(result -> {
                    assertThat(result.estado()).isEqualTo("COMPLETADA");
                    assertThat(result.actualizada()).isTrue();
                })
                .verifyComplete();

        ArgumentCaptor<OperacionAsincrona> captor = ArgumentCaptor.forClass(OperacionAsincrona.class);
        verify(repositoryPort).save(captor.capture());
        assertThat(captor.getValue().estado()).isEqualTo(EstadoOperacionAsincrona.COMPLETADA);
    }

    @Test
    void actualizarFallidaMarcaOperacionComoFallida() {
        OperacionSagaService service = service();
        UUID operationId = UUID.randomUUID();
        when(repositoryPort.findByOperationId(operationId)).thenReturn(Mono.just(operacion(operationId)));
        when(repositoryPort.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.actualizar(new ActualizarOperacionResultadoCommand(
                        operationId,
                        ActualizarOperacionResultadoCommand.Resultado.FALLIDA,
                        "SALDO_INSUFICIENTE",
                        "Saldo insuficiente"
                )))
                .assertNext(result -> {
                    assertThat(result.estado()).isEqualTo("FALLIDA");
                    assertThat(result.actualizada()).isTrue();
                })
                .verifyComplete();

        ArgumentCaptor<OperacionAsincrona> captor = ArgumentCaptor.forClass(OperacionAsincrona.class);
        verify(repositoryPort).save(captor.capture());
        assertThat(captor.getValue().estado()).isEqualTo(EstadoOperacionAsincrona.FALLIDA);
        assertThat(captor.getValue().motivoFallo()).isEqualTo("Saldo insuficiente");
    }

    @Test
    void actualizarResultadoDuplicadoEsIdempotente() {
        OperacionSagaService service = service();
        UUID operationId = UUID.randomUUID();
        when(repositoryPort.findByOperationId(operationId))
                .thenReturn(Mono.just(operacion(operationId).marcarCompletada(LocalDateTime.now())));

        StepVerifier.create(service.actualizar(new ActualizarOperacionResultadoCommand(
                        operationId,
                        ActualizarOperacionResultadoCommand.Resultado.COMPLETADA,
                        null,
                        null
                )))
                .assertNext(result -> {
                    assertThat(result.estado()).isEqualTo("COMPLETADA");
                    assertThat(result.actualizada()).isFalse();
                })
                .verifyComplete();

        verify(repositoryPort, never()).save(any());
    }

    private OperacionSagaService service() {
        return new OperacionSagaService(repositoryPort, publisherPort, comisionCalculatorPort);
    }

    private UUID correlationId() {
        return UUID.fromString("22222222-2222-2222-2222-222222222222");
    }

    private OperacionAsincrona operacion(UUID operationId) {
        LocalDateTime ahora = LocalDateTime.now();
        return new OperacionAsincrona(
                operationId,
                correlationId(),
                "TRANSFERENCIA",
                10L,
                10L,
                11L,
                new BigDecimal("25.00"),
                "EUR",
                EstadoOperacionAsincrona.SOLICITADA,
                null,
                ahora,
                ahora
        );
    }
}
