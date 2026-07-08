package com.novabank.operacion.event;

import com.novabank.events.operacion.OperacionCompletadaEvent;
import com.novabank.events.operacion.OperacionFallidaEvent;
import com.novabank.operacion.adapter.in.kafka.OperacionResultadoEventConsumerConfig;
import com.novabank.operacion.application.port.in.ActualizarEstadoOperacionResultadoUseCase;
import com.novabank.operacion.application.port.in.ActualizarOperacionResultadoCommand;
import com.novabank.operacion.application.port.in.ActualizarOperacionResultadoResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperacionResultadoEventConsumerConfigTest {

    private final ActualizarEstadoOperacionResultadoUseCase useCase =
            mock(ActualizarEstadoOperacionResultadoUseCase.class);
    private final OperacionResultadoEventConsumerConfig config = new OperacionResultadoEventConsumerConfig();

    @Test
    void consumirOperacionCompletadaConvierteEventoAComando() {
        when(useCase.actualizar(any())).thenReturn(Mono.just(new ActualizarOperacionResultadoResult(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "COMPLETADA",
                true
        )));
        Consumer<Flux<org.springframework.messaging.Message<OperacionCompletadaEvent>>> consumer =
                config.consumirOperacionCompletada(useCase);

        consumer.accept(Flux.just(MessageBuilder.withPayload(completada()).build()));

        ArgumentCaptor<ActualizarOperacionResultadoCommand> captor =
                ArgumentCaptor.forClass(ActualizarOperacionResultadoCommand.class);
        verify(useCase, timeout(1000)).actualizar(captor.capture());
        assertThat(captor.getValue().operationId())
                .isEqualTo(UUID.fromString("33333333-3333-3333-3333-333333333333"));
        assertThat(captor.getValue().resultado())
                .isEqualTo(ActualizarOperacionResultadoCommand.Resultado.COMPLETADA);
    }

    @Test
    void consumirOperacionFallidaConvierteEventoAComando() {
        when(useCase.actualizar(any())).thenReturn(Mono.just(new ActualizarOperacionResultadoResult(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "FALLIDA",
                true
        )));
        Consumer<Flux<org.springframework.messaging.Message<OperacionFallidaEvent>>> consumer =
                config.consumirOperacionFallida(useCase);

        consumer.accept(Flux.just(MessageBuilder.withPayload(fallida()).build()));

        ArgumentCaptor<ActualizarOperacionResultadoCommand> captor =
                ArgumentCaptor.forClass(ActualizarOperacionResultadoCommand.class);
        verify(useCase, timeout(1000)).actualizar(captor.capture());
        assertThat(captor.getValue().resultado())
                .isEqualTo(ActualizarOperacionResultadoCommand.Resultado.FALLIDA);
        assertThat(captor.getValue().codigoError()).isEqualTo("SALDO_INSUFICIENTE");
        assertThat(captor.getValue().motivo()).isEqualTo("Saldo insuficiente");
    }

    @Test
    void consumirOperacionInexistenteNoRompeElStream() {
        when(useCase.actualizar(any())).thenReturn(Mono.just(new ActualizarOperacionResultadoResult(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "FALLIDA",
                false
        )));
        Consumer<Flux<org.springframework.messaging.Message<OperacionFallidaEvent>>> consumer =
                config.consumirOperacionFallida(useCase);

        consumer.accept(Flux.just(MessageBuilder.withPayload(fallida()).build()));

        verify(useCase, timeout(1000)).actualizar(any(ActualizarOperacionResultadoCommand.class));
    }

    private OperacionCompletadaEvent completada() {
        return new OperacionCompletadaEvent(
                UUID.randomUUID(),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                Instant.now(),
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "DEPOSITO",
                null,
                new BigDecimal("50.00"),
                "EUR"
        );
    }

    private OperacionFallidaEvent fallida() {
        return new OperacionFallidaEvent(
                UUID.randomUUID(),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                Instant.now(),
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "RETIRADA",
                "SALDO_INSUFICIENTE",
                "Saldo insuficiente"
        );
    }
}
