package com.novabank.cuenta.adapter.in.kafka;

import com.novabank.cuenta.application.port.in.ProcesarOperacionSolicitadaCommand;
import com.novabank.cuenta.application.port.in.ProcesarOperacionSolicitadaResultado;
import com.novabank.cuenta.application.port.in.ProcesarOperacionSolicitadaUseCase;
import com.novabank.cuenta.application.port.out.OperacionResultadoPublisherPort;
import com.novabank.events.operacion.OperacionSolicitadaEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CuentaOperacionEventConsumerConfigTest {

    @Test
    void procesarOperacionConvierteEventoKafkaAComandoDeAplicacion() {
        ProcesarOperacionSolicitadaUseCase useCase = mock(ProcesarOperacionSolicitadaUseCase.class);
        OperacionResultadoPublisherPort publisher = mock(OperacionResultadoPublisherPort.class);
        CuentaOperacionEventConsumerConfig config = new CuentaOperacionEventConsumerConfig();
        OperacionSolicitadaEvent event = operacion();
        ProcesarOperacionSolicitadaCommand expectedCommand = toCommand(event);

        when(useCase.procesar(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Mono.just(ProcesarOperacionSolicitadaResultado.completada(expectedCommand)));
        when(publisher.publicarCompletada(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.empty());

        Consumer<Flux<Message<OperacionSolicitadaEvent>>> consumer = config.procesarOperacion(useCase, publisher);
        consumer.accept(Flux.just(MessageBuilder.withPayload(event).build()));

        ArgumentCaptor<ProcesarOperacionSolicitadaCommand> captor =
                ArgumentCaptor.forClass(ProcesarOperacionSolicitadaCommand.class);
        verify(useCase, timeout(1000)).procesar(captor.capture());

        ProcesarOperacionSolicitadaCommand command = captor.getValue();
        assertThat(command.eventId()).isEqualTo(event.eventId());
        assertThat(command.correlationId()).isEqualTo(event.correlationId());
        assertThat(command.occurredAt()).isEqualTo(event.occurredAt());
        assertThat(command.operationId()).isEqualTo(event.operationId());
        assertThat(command.tipoOperacion()).isEqualTo(event.tipoOperacion());
        assertThat(command.cuentaOrigenId()).isEqualTo(event.cuentaOrigenId());
        assertThat(command.cuentaDestinoId()).isEqualTo(event.cuentaDestinoId());
        assertThat(command.importe()).isEqualByComparingTo(event.importe());
        assertThat(command.moneda()).isEqualTo(event.moneda());
    }

    @Test
    void procesarOperacionPublicaCompletadaCuandoElUseCaseDevuelveResultadoCompletado() {
        ProcesarOperacionSolicitadaUseCase useCase = mock(ProcesarOperacionSolicitadaUseCase.class);
        OperacionResultadoPublisherPort publisher = mock(OperacionResultadoPublisherPort.class);
        CuentaOperacionEventConsumerConfig config = new CuentaOperacionEventConsumerConfig();
        OperacionSolicitadaEvent event = operacion();
        ProcesarOperacionSolicitadaCommand command = toCommand(event);

        when(useCase.procesar(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Mono.just(ProcesarOperacionSolicitadaResultado.completada(command)));
        when(publisher.publicarCompletada(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.empty());

        Consumer<Flux<Message<OperacionSolicitadaEvent>>> consumer = config.procesarOperacion(useCase, publisher);
        consumer.accept(Flux.just(MessageBuilder.withPayload(event).build()));

        ArgumentCaptor<ProcesarOperacionSolicitadaCommand> captor =
                ArgumentCaptor.forClass(ProcesarOperacionSolicitadaCommand.class);
        verify(publisher, timeout(1000)).publicarCompletada(captor.capture());
        assertThat(captor.getValue()).isEqualTo(command);
        verify(publisher, never()).publicarFallida(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void procesarOperacionPublicaFallidaCuandoElUseCaseDevuelveResultadoFallido() {
        ProcesarOperacionSolicitadaUseCase useCase = mock(ProcesarOperacionSolicitadaUseCase.class);
        OperacionResultadoPublisherPort publisher = mock(OperacionResultadoPublisherPort.class);
        CuentaOperacionEventConsumerConfig config = new CuentaOperacionEventConsumerConfig();
        OperacionSolicitadaEvent event = operacion();
        ProcesarOperacionSolicitadaCommand command = toCommand(event);

        when(useCase.procesar(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Mono.just(ProcesarOperacionSolicitadaResultado.fallida(
                        command,
                        "SALDO_INSUFICIENTE",
                        "Saldo insuficiente"
                )));
        when(publisher.publicarFallida(
                org.mockito.ArgumentMatchers.any(),
                eq("SALDO_INSUFICIENTE"),
                eq("Saldo insuficiente")
        )).thenReturn(Mono.empty());

        Consumer<Flux<Message<OperacionSolicitadaEvent>>> consumer = config.procesarOperacion(useCase, publisher);
        consumer.accept(Flux.just(MessageBuilder.withPayload(event).build()));

        ArgumentCaptor<ProcesarOperacionSolicitadaCommand> captor =
                ArgumentCaptor.forClass(ProcesarOperacionSolicitadaCommand.class);
        verify(publisher, timeout(1000)).publicarFallida(
                captor.capture(),
                eq("SALDO_INSUFICIENTE"),
                eq("Saldo insuficiente")
        );
        assertThat(captor.getValue()).isEqualTo(command);
        verify(publisher, never()).publicarCompletada(org.mockito.ArgumentMatchers.any());
    }

    private ProcesarOperacionSolicitadaCommand toCommand(OperacionSolicitadaEvent event) {
        return new ProcesarOperacionSolicitadaCommand(
                event.eventId(),
                event.correlationId(),
                event.occurredAt(),
                event.operationId(),
                event.tipoOperacion(),
                event.cuentaOrigenId(),
                event.cuentaDestinoId(),
                event.importe(),
                event.moneda()
        );
    }

    private OperacionSolicitadaEvent operacion() {
        return new OperacionSolicitadaEvent(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                Instant.parse("2026-06-03T10:15:30Z"),
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "TRANSFERENCIA",
                10L,
                11L,
                new BigDecimal("25.00"),
                "EUR"
        );
    }
}
